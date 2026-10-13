package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.List;

public final class MethodSymbol {
    private final String name;
    private final ZType returnType;
    private final List<ZType> parameterTypes;

    public MethodSymbol(String name, ZType returnType, List<ZType> parameterTypes) {
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    public String getName() { return name; }
    public ZType getReturnType() { return returnType; }
    public int getArity() { return parameterTypes.size(); }
}