package com.usac.contactoalien.contactoalienbackend.semantic.y.symbols;

import java.util.HashMap;
import java.util.Map;

public final class YScope {

    private final YScope parent;
    private final Map<String, YVariableSymbol> variables = new HashMap<>();

    public YScope(YScope parent) {
        this.parent = parent;
    }

    public boolean declare(YVariableSymbol symbol) {
        if (variables.containsKey(symbol.getName())) return false;
        variables.put(symbol.getName(), symbol);
        return true;
    }

    public YVariableSymbol resolve(String name) {
        YScope current = this;
        while (current != null) {
            YVariableSymbol found = current.variables.get(name);
            if (found != null) return found;
            current = current.parent;
        }
        return null;
    }
}