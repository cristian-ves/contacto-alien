package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.List;

public final class MethodSymbol {
    private final String name;
    private final ZType returnType;
    private final List<ZType> parameterTypes;
    private final Visibility visibility;
    private final String ownerClass;
    private final boolean overrideAnnotated;

    public MethodSymbol(String name, ZType returnType, List<ZType> parameterTypes) {
        this(name, returnType, parameterTypes, Visibility.PUBLIC, null, false);
    }

    public MethodSymbol(String name, ZType returnType, List<ZType> parameterTypes, Visibility visibility,
                        String ownerClass, boolean overrideAnnotated) {
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.visibility = visibility;
        this.ownerClass = ownerClass;
        this.overrideAnnotated = overrideAnnotated;
    }

    public String getName() { return name; }
    public ZType getReturnType() { return returnType; }
    public List<ZType> getParameterTypes() { return parameterTypes; }
    public int getArity() { return parameterTypes.size(); }
    public Visibility getVisibility() { return visibility; }
    public String getOwnerClass() { return ownerClass; }
    public boolean isOverrideAnnotated() { return overrideAnnotated; }
}
