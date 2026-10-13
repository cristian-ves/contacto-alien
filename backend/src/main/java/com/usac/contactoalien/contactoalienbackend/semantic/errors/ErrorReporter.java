package com.usac.contactoalien.contactoalienbackend.semantic.errors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Accumulates every semantic error found during analysis instead of stopping at the first one,
// so the whole program can be checked in a single pass.
public final class ErrorReporter {

    private final List<SemanticError> errors = new ArrayList<>();

    public void report(String message, ParserRuleContext ctx) {
        Token start = ctx.getStart();
        errors.add(new SemanticError(message, start.getLine(), start.getCharPositionInLine() + 1));
    }

    public boolean hasErrors() { return !errors.isEmpty(); }

    public List<SemanticError> getErrors() { return Collections.unmodifiableList(errors); }
}