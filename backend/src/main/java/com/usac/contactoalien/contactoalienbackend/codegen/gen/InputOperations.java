package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;

/**
Reading a line of standard input and converting it to the type of the variable that receives it
(`<<` in Pig Latin, `leer` in Y?, a bare `readln()` in Zetariano).
*/
final class InputOperations {

    private static final String LATIN_TRUE = "verum";

    private final QuadBuilder quads;
    private final BooleanText booleans;

    InputOperations(QuadBuilder quads, BooleanText booleans) {
        this.quads = quads;
        this.booleans = booleans;
    }

    // Whether a line of input can be converted to the type (anything scalar except a string).
    boolean isReadable(GenType type) {
        return switch (type.kind()) {
            case INT, FLOAT, CHAR, BOOL -> true;
            default -> false;
        };
    }

    ExprValue read(GenType type) {
        return switch (type.kind()) {
            case FLOAT -> new ExprValue(quads.emitTemp(QuadOp.READ_FLOAT, null, null), GenType.FLOAT);
            case STRING -> new ExprValue(quads.emitTemp(QuadOp.READ_STR, null, null), GenType.STRING);
            case CHAR -> new ExprValue(quads.emitTemp(QuadOp.READ_CHAR, null, null), GenType.CHAR);
            case BOOL -> readBoolean();
            default -> new ExprValue(quads.emitTemp(QuadOp.READ_INT, null, null), GenType.INT);
        };
    }

    // A boolean line is `verum` or, in a language with its own words, that word too.
    private ExprValue readBoolean() {
        String line = quads.emitTemp(QuadOp.READ_STR, null, null);
        String latin = quads.emitTemp(QuadOp.STR_EQ, line, Operand.stringLiteral(LATIN_TRUE));
        if (booleans.fromRuntime()) {
            return new ExprValue(latin, GenType.BOOL);
        }
        String own = quads.emitTemp(QuadOp.STR_EQ, line, Operand.stringLiteral(booleans.trueText()));
        return new ExprValue(quads.emitTemp(QuadOp.OR, latin, own), GenType.BOOL);
    }
}
