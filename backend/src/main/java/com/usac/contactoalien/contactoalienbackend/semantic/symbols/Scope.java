package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import java.util.HashMap;
import java.util.Map;

// A single block/method-level scope. Lookups walk up through parent scopes, but the
// duplicate-declaration check only looks at this scope's own variables (nested blocks may
// legally shadow an outer variable, per the project's stated rule).
public final class Scope {

    private final Scope parent;
    private final Map<String, VariableSymbol> variables = new HashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    public boolean declare(VariableSymbol symbol) {
        if (variables.containsKey(symbol.getName())) return false;
        variables.put(symbol.getName(), symbol);
        return true;
    }

    public VariableSymbol resolve(String name) {
        Scope current = this;
        while (current != null) {
            VariableSymbol found = current.variables.get(name);
            if (found != null) return found;
            current = current.parent;
        }
        return null;
    }
}