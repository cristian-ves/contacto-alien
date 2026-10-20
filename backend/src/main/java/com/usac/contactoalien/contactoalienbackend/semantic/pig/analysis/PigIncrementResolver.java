package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;

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
        if (op.INC() != null) {
            return validateNumeric(current, "++", ctx);
        }
        if (op.DEC() != null) {
            return validateNumeric(current, "--", ctx);
        }
        return current;
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
