package com.usac.contactoalien.contactoalienbackend.codegen.gen.y;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LiteralDecoder;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.OperatorFold;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.Target;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

/**
Translates Y? expressions to quadruples. Operator chains are folded left to right and lowered
by Operations; postfix chains (calls, fields, elements, ++/--) are delegated to YPostfixGen.
leer() has no type of its own: where the destination is known (declaration, assignment,
argument, return) typed() passes it in and the line read is converted to that type.
*/
final class YExpressionGen extends YParserBaseVisitor<ExprValue> {

    private final GenContext gen;
    private final YPostfixGen postfix;
    private final OperatorFold folds;

    YExpressionGen(GenContext gen) {
        this.gen = gen;
        this.postfix = new YPostfixGen(gen, this);
        this.folds = new OperatorFold(gen.ops(), this::visit);
    }

    ExprValue expression(YParser.ExpressionContext ctx) {
        return visit(ctx);
    }

    /**
    Evaluates an expression that will be stored in something of the given type (null when unknown).
    A bare leer() is read as that type; anything else is evaluated normally.
    */
    ExprValue typed(ParserRuleContext ctx, GenType destination) {
        YParser.PrimaryContext primary = OperatorFold.descend(ctx, YParser.PrimaryContext.class);
        if (destination != null && primary != null && primary.LEER() != null) {
            return gen.ops().read(destination);
        }
        return visit(ctx);
    }

    @Override
    public ExprValue visitAssignment(YParser.AssignmentContext ctx) {
        if (ctx.ASSIGN() == null) {
            return visit(ctx.logicalOr());
        }
        YParser.PostfixContext target = OperatorFold.descend(ctx.logicalOr(), YParser.PostfixContext.class);
        if (target == null) {
            throw GenerationException.unsupported(GenerationMessages.FEATURE_ASSIGN_TARGET, ctx);
        }
        Target place = postfix.target(target, target.postfixOp().size());
        return place.write(typed(ctx.assignment(), place.type()));
    }

    @Override
    public ExprValue visitLogicalOr(YParser.LogicalOrContext ctx) {
        return folds.logical(ctx, false);
    }

    @Override
    public ExprValue visitLogicalAnd(YParser.LogicalAndContext ctx) {
        return folds.logical(ctx, true);
    }

    @Override
    public ExprValue visitEquality(YParser.EqualityContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitRelational(YParser.RelationalContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitAdditive(YParser.AdditiveContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitMultiplicative(YParser.MultiplicativeContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitUnary(YParser.UnaryContext ctx) {
        if (ctx.postfix() != null) {
            return visit(ctx.postfix());
        }
        if (ctx.INC() != null || ctx.DEC() != null) {
            YParser.PostfixContext operand = OperatorFold.descend(ctx.unary(), YParser.PostfixContext.class);
            if (operand == null) {
                throw GenerationException.unsupported(GenerationMessages.FEATURE_ASSIGN_TARGET, ctx);
            }
            return postfix.step(postfix.target(operand, operand.postfixOp().size()), ctx.INC() != null, true, ctx);
        }
        ExprValue operand = visit(ctx.unary());
        if (ctx.MINUS() != null) {
            return gen.ops().negate(operand, ctx);
        }
        return ctx.NOT() != null ? gen.ops().not(operand) : operand;
    }

    @Override
    public ExprValue visitPostfix(YParser.PostfixContext ctx) {
        return postfix.evaluate(ctx);
    }

    @Override
    public ExprValue visitPrimary(YParser.PrimaryContext ctx) {
        if (ctx.INT_LITERAL() != null) {
            return new ExprValue(LiteralDecoder.integer(ctx.INT_LITERAL().getText(), ctx), GenType.INT);
        }
        if (ctx.FLOAT_LITERAL() != null) {
            return new ExprValue(Operand.floatLiteral(Double.parseDouble(ctx.FLOAT_LITERAL().getText())), GenType.FLOAT);
        }
        if (ctx.STRING_LITERAL() != null) {
            return new ExprValue(Operand.stringLiteral(LiteralDecoder.unquote(ctx.STRING_LITERAL().getText())),
                    GenType.STRING);
        }
        if (ctx.CHAR_LITERAL() != null) {
            return new ExprValue(LiteralDecoder.charCode(ctx.CHAR_LITERAL().getText()), GenType.CHAR);
        }
        if (ctx.VERDADERO() != null) {
            return new ExprValue("1", GenType.BOOL);
        }
        if (ctx.FALSO() != null) {
            return new ExprValue("0", GenType.BOOL);
        }
        if (ctx.IDENTIFIER() != null) {
            return gen.variable(ctx.IDENTIFIER().getText(), ctx);
        }
        if (ctx.IMPRIMIR() != null) {
            return print(ctx);
        }
        if (ctx.LEER() != null) {
            return gen.ops().read(GenType.INT);
        }
        if (ctx.LBRACE() != null) {
            List<ExprValue> values = new ArrayList<>();
            ctx.expression().forEach(value -> values.add(visit(value)));
            return gen.members().arrayOf(values);
        }
        return visit(ctx.expression(0));
    }

    // imprimir(a, b) prints its arguments in order and ends the line.
    private ExprValue print(YParser.PrimaryContext ctx) {
        for (ExprValue argument : arguments(ctx.argumentList(), List.of())) {
            gen.ops().print(argument, ctx);
        }
        gen.ops().printNewline();
        return new ExprValue(null, GenType.VOID);
    }

    // Each argument is evaluated for the parameter type at the same position, when there is one.
    List<ExprValue> arguments(YParser.ArgumentListContext list, List<GenType> parameters) {
        List<ExprValue> values = new ArrayList<>();
        if (list != null) {
            for (int i = 0; i < list.expression().size(); i++) {
                values.add(typed(list.expression(i), i < parameters.size() ? parameters.get(i) : null));
            }
        }
        return values;
    }
}
