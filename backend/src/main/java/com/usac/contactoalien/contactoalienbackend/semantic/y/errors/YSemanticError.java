package com.usac.contactoalien.contactoalienbackend.semantic.y.errors;

public final class YSemanticError {
    private final String message;
    private final int line;
    private final int column;

    public YSemanticError(String message, int line, int column) {
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public String getMessage() { return message; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    @Override
    public String toString() {
        return "[line " + line + ":" + column + "] " + message;
    }
}