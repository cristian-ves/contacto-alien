package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import org.antlr.v4.runtime.ParserRuleContext;

// A problem found while translating a valid program; carries the source position of the construct.
public class GenerationException extends RuntimeException {

    private final int line;
    private final int column;

    public GenerationException(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public GenerationException(String message, ParserRuleContext at) {
        this(message, at.getStart().getLine(), at.getStart().getCharPositionInLine() + 1);
    }

    public static GenerationException unsupported(String feature, ParserRuleContext at) {
        return new GenerationException(GenerationMessages.notSupported(feature), at);
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
