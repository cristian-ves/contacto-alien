package com.usac.contactoalien.contactoalienbackend.codegen.gen.z;

import com.usac.contactoalien.contactoalienbackend.grammar.ReadlnForms;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LiteralDecoder;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.OperatorFold;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.Target;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

/**
Translates Zetariano expressions to quadruples. Inside a method an unqualified name is a
local variable or parameter first and a field of the current object second; `this` is the
first (hidden) parameter of every method and constructor.
*/
final class ZExpressionGen extends ZetarianoParserBaseVisitor<ExprValue> {

    static final String THIS = "this";

    private final GenContext gen;
    private final OperatorFold folds;
    private final ZMemberGen members;
    private final ZArrayGen arrays;

    ZExpressionGen(GenContext gen, ClassInfo current) {
        this.gen = gen;
        this.folds = new OperatorFold(gen.ops(), this::visit);
        this.members = new ZMemberGen(gen, current, this);
        this.arrays = new ZArrayGen(gen, this);
    }

    ZArrayGen arrays() {
        return arrays;
    }

    ExprValue expression(ZetarianoParser.ExpressionContext ctx) {
        return visit(ctx);
    }

    List<ExprValue> arguments(ZetarianoParser.ArgumentListContext list) {
        List<ExprValue> values = new ArrayList<>();
        if (list != null) {
            for (ZetarianoParser.ExpressionContext argument : list.expression()) {
                values.add(visit(argument));
            }
        }
        return values;
    }

    @Override
    public ExprValue visitAssignmentExpression(ZetarianoParser.AssignmentExpressionContext ctx) {
        if (ctx.assignmentExpression() == null) {
            return visit(ctx.ternaryExpression());
        }
        ZetarianoParser.PostfixExpressionContext postfix =
                OperatorFold.descend(ctx.ternaryExpression(), ZetarianoParser.PostfixExpressionContext.class);
        if (postfix == null) {
            throw GenerationException.unsupported(GenerationMessages.FEATURE_ASSIGN_TARGET, ctx);
        }
        Target target = members.target(postfix, postfix.postfixOp().size());
        ExprValue current = ctx.ASSIGN() == null ? target.read() : null;
        ExprValue value = ctx.ASSIGN() != null && ReadlnForms.isBareCall(ctx.assignmentExpression()) && gen.ops().isReadable(target.type())
                ? gen.ops().read(target.type()) : visit(ctx.assignmentExpression());
        if (current != null) {
            String symbol = ctx.PLUS_ASSIGN() != null ? "+" : ctx.MINUS_ASSIGN() != null ? "-" : "*";
            value = gen.ops().truncate(gen.ops().binary(symbol, current, value, ctx), target.type());
        }
        return target.write(value);
    }

    @Override
    public ExprValue visitTernaryExpression(ZetarianoParser.TernaryExpressionContext ctx) {
        if (ctx.QUESTION() == null) {
            return visit(ctx.logicalOrExpression());
        }
        return gen.ops().conditional(() -> visit(ctx.logicalOrExpression()),
                () -> visit(ctx.expression(0)), () -> visit(ctx.expression(1)));
    }

    @Override
    public ExprValue visitLogicalOrExpression(ZetarianoParser.LogicalOrExpressionContext ctx) {
        return folds.logical(ctx, false);
    }

    @Override
    public ExprValue visitLogicalAndExpression(ZetarianoParser.LogicalAndExpressionContext ctx) {
        return folds.logical(ctx, true);
    }

    @Override
    public ExprValue visitEqualityExpression(ZetarianoParser.EqualityExpressionContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitRelationalExpression(ZetarianoParser.RelationalExpressionContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitAdditiveExpression(ZetarianoParser.AdditiveExpressionContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitMultiplicativeExpression(ZetarianoParser.MultiplicativeExpressionContext ctx) {
        return folds.binary(ctx);
    }

    @Override
    public ExprValue visitUnaryExpression(ZetarianoParser.UnaryExpressionContext ctx) {
        if (ctx.postfixExpression() != null) {
            return visit(ctx.postfixExpression());
        }
        if (ctx.castType() != null) {
            return cast(ctx);
        }
        if (ctx.PLUS_PLUS() != null || ctx.MINUS_MINUS() != null) {
            ZetarianoParser.PostfixExpressionContext operand =
                    OperatorFold.descend(ctx.unaryExpression(), ZetarianoParser.PostfixExpressionContext.class);
            if (operand == null) {
                throw GenerationException.unsupported(GenerationMessages.FEATURE_ASSIGN_TARGET, ctx);
            }
            Target target = members.target(operand, operand.postfixOp().size());
            return members.step(target, ctx.PLUS_PLUS() != null, false, ctx);
        }
        ExprValue operand = visit(ctx.unaryExpression());
        if (ctx.MINUS() != null) {
            return gen.ops().negate(operand, ctx);
        }
        return ctx.NOT() != null ? gen.ops().not(operand) : operand;
    }

    // (double) widens; (int) and (char) drop the fractional part of a decimal and relabel the integer code.
    private ExprValue cast(ZetarianoParser.UnaryExpressionContext ctx) {
        ExprValue operand = visit(ctx.unaryExpression());
        if (ctx.castType().DOUBLE() != null) {
            return gen.ops().coerce(operand, GenType.FLOAT);
        }
        GenType target = ctx.castType().INT() != null ? GenType.INT : GenType.CHAR;
        return new ExprValue(gen.ops().truncate(operand, target).operand(), target);
    }

    @Override
    public ExprValue visitPostfixExpression(ZetarianoParser.PostfixExpressionContext ctx) {
        return members.postfix(ctx);
    }

    @Override
    public ExprValue visitPrimaryExpression(ZetarianoParser.PrimaryExpressionContext ctx) {
        if (ctx.INT_LITERAL() != null) {
            return new ExprValue(LiteralDecoder.integer(ctx.INT_LITERAL().getText(), ctx), GenType.INT);
        }
        if (ctx.DOUBLE_LITERAL() != null) {
            return new ExprValue(Operand.floatLiteral(Double.parseDouble(ctx.DOUBLE_LITERAL().getText())), GenType.FLOAT);
        }
        if (ctx.STRING_LITERAL() != null) {
            return new ExprValue(Operand.stringLiteral(LiteralDecoder.unquote(ctx.STRING_LITERAL().getText())),
                    GenType.STRING);
        }
        if (ctx.CHAR_LITERAL() != null) {
            return new ExprValue(LiteralDecoder.charCode(ctx.CHAR_LITERAL().getText()), GenType.CHAR);
        }
        if (ctx.TRUE() != null || ctx.FALSE() != null) {
            return new ExprValue(ctx.TRUE() != null ? "1" : "0", GenType.BOOL);
        }
        if (ctx.NULL() != null) {
            return new ExprValue("0", GenType.NULL);
        }
        if (ctx.THIS() != null) {
            return members.self(ctx);
        }
        if (ctx.SUPER() != null) {
            return members.superReference(ctx);
        }
        if (ctx.NEW() != null) {
            return members.instantiate(ctx);
        }
        if (ctx.IDENTIFIER() != null) {
            return members.targetOf(ctx.IDENTIFIER().getText(), ctx).read();
        }
        if (ctx.arrayCreation() != null) {
            return arrays.create(ctx.arrayCreation());
        }
        if (ctx.READLN() != null) {
            return gen.ops().read(GenType.STRING);
        }
        return visit(ctx.expression());
    }

    // The constant after `case`: an integer, character, string or boolean literal.
    ExprValue caseLabel(ZetarianoParser.SwitchLabelContext label) {
        if (label.INT_LITERAL() != null) {
            return new ExprValue(LiteralDecoder.integer(label.INT_LITERAL().getText(), label), GenType.INT);
        }
        if (label.CHAR_LITERAL() != null) {
            return new ExprValue(LiteralDecoder.charCode(label.CHAR_LITERAL().getText()), GenType.CHAR);
        }
        if (label.STRING_LITERAL() != null) {
            return new ExprValue(Operand.stringLiteral(LiteralDecoder.unquote(label.STRING_LITERAL().getText())),
                    GenType.STRING);
        }
        return new ExprValue(label.TRUE() != null ? "1" : "0", GenType.BOOL);
    }
}
