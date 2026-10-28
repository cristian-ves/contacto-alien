package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;

import java.util.List;

/**
Validates postfix increment (++) and decrement (--) operations in Pig Latin, ensuring
the operand is numeric (numerus or decimalis) and reporting a typed semantic error otherwise.
*/
public final class PigIncrementResolver {

    private final PigErrorReporter reporter;

    public PigIncrementResolver(PigErrorReporter reporter) {
        this.reporter = reporter;
    }

    public PigType resolve(PigParser.PostfixOpContext op, PigType current, PigParser.PostfixExpressionContext ctx) {
        if ((op.INC() != null || op.DEC() != null) && !isPlace(ctx, op)) {
            reporter.report("the operand of '" + op.getText() + "' must be a variable, an object field or a series element", ctx);
        }
        if (op.INC() != null) {
            return validateNumeric(current, "++", ctx);
        }
        if (op.DEC() != null) {
            return validateNumeric(current, "--", ctx);
        }
        return current;
    }

    // The primary followed by the operators before `op` must name a variable, a field or an element.
    private static boolean isPlace(PigParser.PostfixExpressionContext ctx, PigParser.PostfixOpContext op) {
        List<PigParser.PostfixOpContext> ops = ctx.postfixOp();
        int count = ops.indexOf(op);
        if (count == 0) {
            return ctx.primaryExpression().IDENTIFIER() != null && ctx.primaryExpression().NOVUS() == null;
        }
        PigParser.PostfixOpContext last = ops.get(count - 1);
        boolean callName = last.DOT() != null && count < ops.size() && ops.get(count).LPAREN() != null;
        return (last.DOT() != null && !callName) || last.LBRACK() != null;
    }

    private PigType validateNumeric(PigType current, String opSymbol, PigParser.PostfixExpressionContext ctx) {
        if (current.isUnknown()) {
            return PigType.UNKNOWN;
        }
        if (!current.isNumeric()) {
            reporter.report("operator '" + opSymbol + "' requires a numeric operand, found " + current, ctx);
            return PigType.UNKNOWN;
        }
        return current;
    }
}
