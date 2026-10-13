package com.usac.contactoalien.contactoalienbackend.semantic.y.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;

public final class YVariableSymbol {
    private final String name;
    private final YType type;

    public YVariableSymbol(String name, YType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public YType getType() { return type; }
}