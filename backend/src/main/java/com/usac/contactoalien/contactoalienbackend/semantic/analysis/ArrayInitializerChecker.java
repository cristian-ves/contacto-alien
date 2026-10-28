package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.VariableInitializerContext;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

/**
Checks the initializer of a local variable declaration against the declared type. A plain
expression must be assignable; a braced list is only valid for an array type, and each of its
entries is checked against the element type, so `int[][] m = {{1, 2}, {3}}` descends one
dimension per brace level.
*/
public final class ArrayInitializerChecker {

    private final ExpressionTypeResolver expressionResolver;
    private final ErrorReporter errors;

    public ArrayInitializerChecker(ExpressionTypeResolver expressionResolver, ErrorReporter errors) {
        this.expressionResolver = expressionResolver;
        this.errors = errors;
    }

    public void check(String name, ZType declared, VariableInitializerContext initializer) {
        if (initializer.expression() != null) {
            ZType valueType = expressionResolver.resolve(initializer.expression(), declared);
            if (!expressionResolver.isAssignable(declared, valueType)) {
                errors.report("Cannot initialize variable '" + name + "' of type '" + declared
                        + "' with value of type '" + valueType + "'", initializer);
            }
            return;
        }
        if (!declared.isArray()) {
            if (!declared.isUnknown()) {
                errors.report("Cannot initialize variable '" + name + "' of type '" + declared
                        + "' with an array initializer", initializer);
            }
            return;
        }
        ZType element = declared.asArray(-1);
        for (VariableInitializerContext entry : initializer.arrayInitializer().variableInitializer()) {
            check(name, element, entry);
        }
    }
}
