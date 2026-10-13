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
}