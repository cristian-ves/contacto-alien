package com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

// Symbol table for Pig Latin supporting nested lexical scopes (e.g. for-loop counters).
// Everything declared in VARIABILES> lives in the root scope and is visible everywhere.
public final class PigSymbolTable {

    private final Deque<Map<String, PigVariableSymbol>> scopes = new ArrayDeque<>();

    public PigSymbolTable() {
        scopes.push(new LinkedHashMap<>());
    }

    public void enterScope() {
        scopes.push(new LinkedHashMap<>());
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
    }

    public boolean declare(PigVariableSymbol variable) {
        Map<String, PigVariableSymbol> currentScope = scopes.peek();
        if (currentScope != null && currentScope.containsKey(variable.getName())) {
            return false;
        }
        if (currentScope != null) {
            currentScope.put(variable.getName(), variable);
        }
        return true;
    }

    public PigVariableSymbol resolve(String name) {
        for (Map<String, PigVariableSymbol> scope : scopes) {
            PigVariableSymbol symbol = scope.get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }
}