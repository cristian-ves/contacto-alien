package com.usac.contactoalien.contactoalienbackend.semantic.y.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;

public final class YVariableSymbol {
    private final String name;
    private final YType type;
    // Declared element count of a one-dimensional array, or -1 when it is not known.
    private final int length;

    public YVariableSymbol(String name, YType type) {
        this(name, type, -1);
    }

    public YVariableSymbol(String name, YType type, int length) {
        this.name = name;
        this.type = type;
        this.length = length;
    }

    public String getName() { return name; }
    public YType getType() { return type; }
    public int getLength() { return length; }
}