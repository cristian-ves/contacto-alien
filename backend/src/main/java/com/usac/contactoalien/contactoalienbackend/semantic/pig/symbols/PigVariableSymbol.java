package com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;

public final class PigVariableSymbol {
    private final String name;
    private final PigType type;
    // Declared element count of a series, or -1 when it is not known.
    private final int length;

    public PigVariableSymbol(String name, PigType type) {
        this(name, type, -1);
    }

    public PigVariableSymbol(String name, PigType type, int length) {
        this.name = name;
        this.type = type;
        this.length = length;
    }

    public String getName() { return name; }
    public PigType getType() { return type; }
    public int getLength() { return length; }
}