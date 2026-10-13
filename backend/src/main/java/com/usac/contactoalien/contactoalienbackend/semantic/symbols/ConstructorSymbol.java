package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.List;

public final class ConstructorSymbol {
    private final List<ZType> parameterTypes;

    public ConstructorSymbol(List<ZType> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public int getArity() { return parameterTypes.size(); }
}