package com.usac.contactoalien.contactoalienbackend.semantic.y.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;

import java.util.List;

public final class YFunctionSymbol {
    private final String name;
    private final List<YType> parameterTypes;
    private final YType returnType;

    public YFunctionSymbol(String name, List<YType> parameterTypes, YType returnType) {
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public String getName() { return name; }
    public YType getReturnType() { return returnType; }
    public List<YType> getParameterTypes() { return parameterTypes; }
    public int getArity() { return parameterTypes.size(); }
}