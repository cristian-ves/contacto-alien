package com.usac.contactoalien.contactoalienbackend.semantic.y.symbols;

import java.util.HashMap;
import java.util.Map;

// Global registry of every structure declared anywhere in the program (top-level, nested
// inside another structure, or declared locally inside a function body).
public final class YStructRegistry {

    private final Map<String, YStructSymbol> structs = new HashMap<>();

    public boolean declare(String name) {
        if (structs.containsKey(name)) return false;
        structs.put(name, new YStructSymbol(name));
        return true;
    }

    public YStructSymbol resolve(String name) { return structs.get(name); }

    public boolean exists(String name) { return structs.containsKey(name); }
}