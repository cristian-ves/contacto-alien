package com.usac.contactoalien.contactoalienbackend.semantic.types;

import java.util.Objects;

// Represents a Zetariano type: a primitive, void, the class type (only one class exists per
// analysis, since the grammar parses exactly one class per file), null, or an unknown/error
// placeholder used to suppress cascading errors after a previous mistake.
public final class ZType {

    public enum Kind { INT, DOUBLE, CHAR, BOOLEAN, STRING, VOID, CLASS, NULL, UNKNOWN }

    public static final ZType INT = new ZType(Kind.INT, null, 0);
    public static final ZType DOUBLE = new ZType(Kind.DOUBLE, null, 0);
    public static final ZType CHAR = new ZType(Kind.CHAR, null, 0);
    public static final ZType BOOLEAN = new ZType(Kind.BOOLEAN, null, 0);
    public static final ZType STRING = new ZType(Kind.STRING, null, 0);
    public static final ZType VOID = new ZType(Kind.VOID, null, 0);
    public static final ZType NULL = new ZType(Kind.NULL, null, 0);
    public static final ZType UNKNOWN = new ZType(Kind.UNKNOWN, null, 0);

    private final Kind kind;
    private final String className;
    private final int arrayDimensions;

    private ZType(Kind kind, String className, int arrayDimensions) {
        this.kind = kind;
        this.className = className;
        this.arrayDimensions = arrayDimensions;
    }

    public static ZType ofClass(String name) {
        return new ZType(Kind.CLASS, name, 0);
    }

    public ZType asArray(int extraDimensions) {
        return new ZType(this.kind, this.className, this.arrayDimensions + extraDimensions);
    }

    // The type of one element: the same type with one dimension fewer.
    public ZType elementType() {
        return new ZType(this.kind, this.className, Math.max(0, this.arrayDimensions - 1));
    }

    public Kind getKind() { return kind; }
    public String getClassName() { return className; }
    public int getArrayDimensions() { return arrayDimensions; }
    public boolean isArray() { return arrayDimensions > 0; }
    // char takes part in arithmetic and comparisons as its character code, as in Java.
    public boolean isNumeric() { return !isArray() && (kind == Kind.INT || kind == Kind.DOUBLE || kind == Kind.CHAR); }
    public boolean isUnknown() { return kind == Kind.UNKNOWN; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZType other)) return false;
        return kind == other.kind
                && arrayDimensions == other.arrayDimensions
                && Objects.equals(className, other.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, className, arrayDimensions);
    }

    @Override
    public String toString() {
        String base = switch (kind) {
            case CLASS -> className;
            case UNKNOWN -> "<unknown>";
            default -> kind.name().toLowerCase();
        };
        return base + "[]".repeat(arrayDimensions);
    }
}