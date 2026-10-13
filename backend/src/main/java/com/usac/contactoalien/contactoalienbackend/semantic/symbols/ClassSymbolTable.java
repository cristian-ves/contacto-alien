package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Class-level scope: fields, overloaded methods (by name) and constructors declared for the
// single class under analysis.
public final class ClassSymbolTable {

    private final String className;
    private final Map<String, VariableSymbol> fields = new HashMap<>();
    private final Map<String, List<MethodSymbol>> methodsByName = new HashMap<>();
    private final List<ConstructorSymbol> constructors = new ArrayList<>();

    public ClassSymbolTable(String className) {
        this.className = className;
    }

    public String getClassName() { return className; }

    public boolean declareField(VariableSymbol field) {
        if (fields.containsKey(field.getName())) return false;
        fields.put(field.getName(), field);
        return true;
    }

    public VariableSymbol resolveField(String name) { return fields.get(name); }

    public void addMethod(MethodSymbol method) {
        methodsByName.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);
    }

    public List<MethodSymbol> resolveMethods(String name) {
        return methodsByName.getOrDefault(name, List.of());
    }

    public void addConstructor(ConstructorSymbol constructor) {
        constructors.add(constructor);
    }

    public List<ConstructorSymbol> getConstructors() { return constructors; }

    public ZType getOwnType() { return ZType.ofClass(className); }
}