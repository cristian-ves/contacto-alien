package com.usac.contactoalien.contactoalienbackend.semantic.y.symbols;

import java.util.HashMap;
import java.util.Map;

// Global registry of every function declared in %funciones. Function names are unique
public final class YFunctionRegistry {

    private final Map<String, YFunctionSymbol> functions = new HashMap<>();

    public boolean declare(YFunctionSymbol function) {
        if (functions.containsKey(function.getName())) return false;
        functions.put(function.getName(), function);
        return true;
    }

    public YFunctionSymbol resolve(String name) { return functions.get(name); }
}