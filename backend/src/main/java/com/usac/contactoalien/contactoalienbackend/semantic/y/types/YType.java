package com.usac.contactoalien.contactoalienbackend.semantic.y.types;

import java.util.Objects;

public final class YType {

    public enum Kind { ENTERO, FLOTANTE, CARACTER, BOOL, CADENA, STRUCT, VOID, UNKNOWN }

    public static final YType ENTERO = new YType(Kind.ENTERO, null, 0);
    public static final YType FLOTANTE = new YType(Kind.FLOTANTE, null, 0);
    public static final YType CARACTER = new YType(Kind.CARACTER, null, 0);
    public static final YType BOOL = new YType(Kind.BOOL, null, 0);
    public static final YType CADENA = new YType(Kind.CADENA, null, 0);
    public static final YType VOID = new YType(Kind.VOID, null, 0);
    public static final YType UNKNOWN = new YType(Kind.UNKNOWN, null, 0);

    private final Kind kind;
    private final String structName;
    private final int arrayDimensions;

    private YType(Kind kind, String structName, int arrayDimensions) {
        this.kind = kind;
        this.structName = structName;
        this.arrayDimensions = arrayDimensions;
    }

    public static YType ofStruct(String name) {
        return new YType(Kind.STRUCT, name, 0);
    }

    public YType asArray(int extraDimensions) {
        return new YType(this.kind, this.structName, this.arrayDimensions + extraDimensions);
    }

    // The type of one element: the same type with one dimension fewer.
    public YType elementType() {
        return new YType(this.kind, this.structName, Math.max(0, this.arrayDimensions - 1));
    }

    public Kind getKind() { return kind; }
    public String getStructName() { return structName; }
    public int getArrayDimensions() { return arrayDimensions; }
    public boolean isArray() { return arrayDimensions > 0; }
    public boolean isNumeric() { return !isArray() && (kind == Kind.ENTERO || kind == Kind.FLOTANTE); }
    public boolean isUnknown() { return kind == Kind.UNKNOWN; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YType other)) return false;
        return kind == other.kind
                && arrayDimensions == other.arrayDimensions
                && Objects.equals(structName, other.structName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, structName, arrayDimensions);
    }

    @Override
    public String toString() {
        String base = switch (kind) {
            case STRUCT -> structName;
            case UNKNOWN -> "<desconocido>";
            default -> kind.name().toLowerCase();
        };
        return base + "[]".repeat(arrayDimensions);
    }
}