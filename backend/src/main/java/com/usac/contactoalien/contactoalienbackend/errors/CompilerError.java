package com.usac.contactoalien.contactoalienbackend.errors; // Immutable representation of a single lexical or syntactic error found by ANTLR4.
public record CompilerError(int line, int column, String message, ErrorPhase phase) {
}