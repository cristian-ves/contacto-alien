package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

public final class VariableSymbol {
    private final String name;
    private final ZType type;

    public VariableSymbol(String name, ZType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public ZType getType() { return type; }
}