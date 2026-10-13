package com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols;

import java.util.LinkedHashMap;
import java.util.Map;

// Single global symbol table for Pig Latin: everything declared in VARIABILES> (variables,
// series, structure instances, object instances) is visible everywhere in MAIOR>, with no
// additional nested scopes for si/dum/facere/per blocks or for a for-loop's own counter
// declaration. Decision documented in PigSemanticAnalyzer.
public final class PigSymbolTable {

    private final Map<String, PigVariableSymbol> variables = new LinkedHashMap<>();

    public boolean declare(PigVariableSymbol variable) {
        if (variables.containsKey(variable.getName())) return false;
        variables.put(variable.getName(), variable);
        return true;
    }

    public PigVariableSymbol resolve(String name) { return variables.get(name); }
}