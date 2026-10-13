package com.usac.contactoalien.contactoalienbackend.semantic.pig.errors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Accumulates every semantic error found while analyzing a Pig Latin body, instead of stopping
// at the first one, so the whole file can be checked in a single pass.
public final class PigErrorReporter {

    private final List<PigSemanticError> errors = new ArrayList<>();

    public void report(String message, ParserRuleContext ctx) {
        Token start = ctx.getStart();
        errors.add(new PigSemanticError(message, start.getLine(), start.getCharPositionInLine() + 1));
    }

    public boolean hasErrors() { return !errors.isEmpty(); }

    public List<PigSemanticError> getErrors() { return Collections.unmodifiableList(errors); }
}