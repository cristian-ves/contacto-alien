package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.function.Function;

/**
Walks the flat `operand (op operand)*` shape that the three grammars use for every binary
precedence level, so each front end only supplies how to evaluate one operand.
*/
public final class OperatorFold {

    private final Operations ops;
    private final Function<ParseTree, ExprValue> evaluate;
    private final boolean charStaysChar;

    public OperatorFold(Operations ops, Function<ParseTree, ExprValue> evaluate) {
        this(ops, evaluate, false);
    }

    // charStaysChar: arithmetic between two chars is a char (Pig Latin takes the highest type of the
    // operands, and littera is its own level); without it the result is an int, as in Java.
    public OperatorFold(Operations ops, Function<ParseTree, ExprValue> evaluate, boolean charStaysChar) {
        this.ops = ops;
        this.evaluate = evaluate;
        this.charStaysChar = charStaysChar;
    }

    // Arithmetic, relational and equality levels: left to right, one lowering per operator token.
    public ExprValue binary(ParserRuleContext ctx) {
        ExprValue accumulated = evaluate.apply(ctx.getChild(0));
        for (int i = 1; i + 1 < ctx.getChildCount(); i += 2) {
            String symbol = ctx.getChild(i).getText();
            ExprValue right = evaluate.apply(ctx.getChild(i + 1));
            boolean bothChars = accumulated.type().kind() == GenType.Kind.CHAR && right.type().kind() == GenType.Kind.CHAR;
            accumulated = ops.binary(symbol, accumulated, right, ctx);
            if (charStaysChar && bothChars && accumulated.type().kind() == GenType.Kind.INT) {
                accumulated = new ExprValue(accumulated.operand(), GenType.CHAR);
            }
        }
        return accumulated;
    }

    // && and || levels, with short-circuit evaluation.
    public ExprValue logical(ParserRuleContext ctx, boolean isAnd) {
        ExprValue accumulated = evaluate.apply(ctx.getChild(0));
        for (int i = 2; i < ctx.getChildCount(); i += 2) {
            ExprValue left = accumulated;
            ParseTree next = ctx.getChild(i);
            accumulated = ops.logical(isAnd, () -> left, () -> evaluate.apply(next));
        }
        return accumulated;
    }

    // Finds the node of the given type at the bottom of a chain of single-child rules, or null.
    public static <T extends ParserRuleContext> T descend(ParserRuleContext ctx, Class<T> type) {
        ParseTree node = ctx;
        while (node instanceof ParserRuleContext rule) {
            if (type.isInstance(rule)) {
                return type.cast(rule);
            }
            if (rule.getChildCount() != 1) {
                return null;
            }
            node = rule.getChild(0);
        }
        return null;
    }
}
