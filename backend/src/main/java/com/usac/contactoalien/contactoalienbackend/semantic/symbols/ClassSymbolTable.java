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
    private final List<MethodSymbol> methodsInOrder = new ArrayList<>();
    private final List<String> declaredFieldNames = new ArrayList<>();
    private String superName;
    private String packageName = "";

    public ClassSymbolTable(String className) {
        this.className = className;
    }

    public String getClassName() { return className; }

    // Name of the class this one extends, or null.
    public String getSuperName() { return superName; }
    public void setSuperName(String superName) { this.superName = superName; }

    // The package is the folder the class file lives in ("" for the root).
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName == null ? "" : packageName; }

    public boolean declareField(VariableSymbol field) {
        if (fields.containsKey(field.getName())) return false;
        fields.put(field.getName(), field);
        declaredFieldNames.add(field.getName());
        return true;
    }

    public VariableSymbol resolveField(String name) { return fields.get(name); }

    // Fields declared by this class only, in source order (inherited ones are not included).
    public List<VariableSymbol> getDeclaredFields() {
        return declaredFieldNames.stream().map(fields::get).toList();
    }

    // Methods declared by this class only, in source order.
    public List<MethodSymbol> getDeclaredMethods() {
        return List.copyOf(methodsInOrder);
    }

    public void addMethod(MethodSymbol method) {
        methodsByName.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);
        methodsInOrder.add(method);
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