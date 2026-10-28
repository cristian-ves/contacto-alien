package com.usac.contactoalien.contactoalienbackend.codegen.gen.pig;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.Target;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LiteralDecoder;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.OperatorFold;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
Translates Pig Latin expressions to quadruples. Operator chains are folded left to right and
lowered by Operations; postfix chains (calls, members, ++/--) are delegated to PigPostfixGen.
*/
final class PigExpressionGen extends PigParserBaseVisitor<ExprValue> {

    private final GenContext gen;
    private final PigPostfixGen postfix;
    private final OperatorFold folds;

    PigExpressionGen(GenContext gen) {
        this.gen = gen;
        this.postfix = new PigPostfixGen(gen, this);
        this.folds = new OperatorFold(gen.ops(), this::visit, true);
    }

    ExprValue expression(PigParser.ExpressionContext ctx) {
        return visit(ctx);
    }

    ExprValue primary(PigParser.PrimaryExpressionContext ctx) {
        return visit(ctx);
    }

    @Override
    public ExprValue visitAssignmentExpr(PigParser.AssignmentExprContext ctx) {
        if (ctx.ASSIGN() == null) {
            return visit(ctx.logicalOrExpr());
        }
        PigParser.PostfixExpressionContext target = OperatorFold.descend(ctx.logicalOrExpr(), PigParser.PostfixExpressionContext.class);
        if (target == null) {
            throw GenerationException.unsupported(GenerationMessages.FEATURE_ASSIGN_TARGET, ctx);
        }
        return postfix.assignTo(target, visit(ctx.assignmentExpr()));
    }

    @Override
    public ExprValue visitLogicalOrExpr(PigParser.LogicalOrExprContext ctx) {
        return folds.logical(ctx, false);
    }

    @Override
    public ExprValue visitLogicalAndExpr(PigParser.LogicalAndExprContext ctx) {
        return folds.logical(ctx, true);
    }

    @Override
    public ExprValue visitEqualityExpr(PigParser.EqualityExprContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitRelationalExpr(PigParser.RelationalExprContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitAdditiveExpr(PigParser.AdditiveExprContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitMultiplicativeExpr(PigParser.MultiplicativeExprContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitUnaryExpr(PigParser.UnaryExprContext ctx) {
        if (ctx.postfixExpression() != null) {
            return visit(ctx.postfixExpression());
        }
        ExprValue operand = visit(ctx.unaryExpr());
        return switch (ctx.getChild(0).getText()) {
            case "-" -> gen.ops().negate(operand, ctx);
            case "+" -> operand;
            default -> gen.ops().not(operand);
        };
    }

    @Override
    public ExprValue visitPostfixExpression(PigParser.PostfixExpressionContext ctx) {
        return postfix.evaluate(ctx, ctx.postfixOp().size());
    }

    @Override
    public ExprValue visitPrimaryExpression(PigParser.PrimaryExpressionContext ctx) {
        if (ctx.INT_LITERAL() != null) {
            return new ExprValue(LiteralDecoder.integer(ctx.INT_LITERAL().getText(), ctx), GenType.INT);
        }
        if (ctx.STRING_LITERAL() != null) {
            String text = LiteralDecoder.unquote(ctx.STRING_LITERAL().getText());
            return new ExprValue(Operand.stringLiteral(text), GenType.STRING);
        }
        if (ctx.CHAR_LITERAL() != null) {
            return new ExprValue(LiteralDecoder.charCode(ctx.CHAR_LITERAL().getText()), GenType.CHAR);
        }
        if (ctx.VERUM() != null) {
            return new ExprValue("1", GenType.BOOL);
        }
        if (ctx.FALSUS() != null) {
            return new ExprValue("0", GenType.BOOL);
        }
        if (ctx.NULL() != null) {
            return new ExprValue("0", GenType.NULL);
        }
        if (ctx.IDENTIFIER() != null && ctx.NOVUS() == null) {
            return gen.variable(ctx.IDENTIFIER().getText(), ctx);
        }
        if (ctx.NOVUS() != null) {
            return postfix.instantiate(ctx.IDENTIFIER().getText(), ctx.argumentList(), ctx);
        }
        if (!ctx.expression().isEmpty() && ctx.LBRACE() == null) {
            return visit(ctx.expression(0));
        }
        if (ctx.FLOAT_LITERAL() != null) {
            double value = Double.parseDouble(ctx.FLOAT_LITERAL().getText());
            return new ExprValue(Operand.floatLiteral(value), GenType.FLOAT);
        }
        return gen.members().arrayOf(ctx.expression().stream().map(this::expression).toList());
    }

    ExprValue assignTo(PigParser.PostfixExpressionContext target, ExprValue value) {
        return postfix.assignTo(target, value);
    }

    Target place(PigParser.PostfixExpressionContext target) {
        return postfix.place(target);
    }

    ExprValue instantiate(String className, PigParser.ArgumentListContext args, ParserRuleContext at) {
        return postfix.instantiate(className, args, at);
    }

    List<ExprValue> arguments(PigParser.ArgumentListContext list) {
        return postfix.arguments(list);
    }
}
