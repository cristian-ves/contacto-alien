package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import java.util.LinkedHashMap;
import java.util.Map;

// Registry of every class-level symbol table available in one compilation batch, indexed by
// class name. Lets a class being analyzed resolve any other class in the same batch as a known
// type, in addition to itself.
public final class CompilationUnitRegistry {

    private final Map<String, ClassSymbolTable> classesByName = new LinkedHashMap<>();

    public void register(ClassSymbolTable classSymbolTable) {
        classesByName.put(classSymbolTable.getClassName(), classSymbolTable);
    }

    public boolean contains(String className) {
        return classesByName.containsKey(className);
    }

    public ClassSymbolTable resolve(String className) {
        return classesByName.get(className);
    }

    // True when sub is the same class as sup or inherits from it, directly or through other classes.
    // The walk stops after as many steps as there are classes, so a cyclic declaration cannot loop.
    public boolean isSubclass(String sub, String sup) {
        String current = sub;
        for (int steps = 0; current != null && steps <= classesByName.size(); steps++) {
            if (current.equals(sup)) return true;
            ClassSymbolTable table = classesByName.get(current);
            current = table == null ? null : table.getSuperName();
        }
        return false;
    }
}