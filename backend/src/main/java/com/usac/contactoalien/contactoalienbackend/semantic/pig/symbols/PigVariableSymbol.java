package com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;

public final class PigVariableSymbol {
    private final String name;
    private final PigType type;

    public PigVariableSymbol(String name, PigType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public PigType getType() { return type; }
}