package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.List;

public final class ConstructorSymbol {
    private final List<ZType> parameterTypes;
    private final Visibility visibility;

    public ConstructorSymbol(List<ZType> parameterTypes) {
        this(parameterTypes, Visibility.PUBLIC);
    }

    public ConstructorSymbol(List<ZType> parameterTypes, Visibility visibility) {
        this.parameterTypes = parameterTypes;
        this.visibility = visibility;
    }

    public List<ZType> getParameterTypes() { return parameterTypes; }
    public int getArity() { return parameterTypes.size(); }
    public Visibility getVisibility() { return visibility; }
}
