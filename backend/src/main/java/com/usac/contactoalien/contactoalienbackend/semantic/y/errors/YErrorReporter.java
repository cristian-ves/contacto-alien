package com.usac.contactoalien.contactoalienbackend.semantic.y.errors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class YErrorReporter {

    private final List<YSemanticError> errors = new ArrayList<>();

    public void report(String message, ParserRuleContext ctx) {
        Token start = ctx.getStart();
        errors.add(new YSemanticError(message, start.getLine(), start.getCharPositionInLine() + 1));
    }

    public boolean hasErrors() { return !errors.isEmpty(); }

    public List<YSemanticError> getErrors() { return Collections.unmodifiableList(errors); }
}