package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

public final class VariableSymbol {
    private final String name;
    private final ZType type;
    private final Visibility visibility;
    private final String ownerClass;

    // Local variables and parameters: no owner and no access level.
    public VariableSymbol(String name, ZType type) {
        this(name, type, Visibility.PUBLIC, null);
    }

    // Fields: the class that declares them and their access level.
    public VariableSymbol(String name, ZType type, Visibility visibility, String ownerClass) {
        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.ownerClass = ownerClass;
    }

    public String getName() { return name; }
    public ZType getType() { return type; }
    public Visibility getVisibility() { return visibility; }
    public String getOwnerClass() { return ownerClass; }
}
