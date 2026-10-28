package com.usac.contactoalien.contactoalienbackend.semantic.pig.errors;

import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;

// An error found while analyzing a Pig Latin program or one of its imports. Errors that come from
// an imported file that failed to parse keep their lexical or syntactic phase.
public final class PigSemanticError {
    private final String message;
    private final int line;
    private final int column;
    private final ErrorPhase phase;

    public PigSemanticError(String message, int line, int column) {
        this(message, line, column, ErrorPhase.SEMANTIC);
    }

    public PigSemanticError(String message, int line, int column, ErrorPhase phase) {
        // Messages of the three languages start with a capital letter.
        this.message = message.isEmpty() ? message : Character.toUpperCase(message.charAt(0)) + message.substring(1);
        this.line = line;
        this.column = column;
        this.phase = phase;
    }

    public String getMessage() { return message; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public ErrorPhase getPhase() { return phase; }

    @Override
    public String toString() {
        return "[line " + line + ":" + column + "] " + message;
    }
}
