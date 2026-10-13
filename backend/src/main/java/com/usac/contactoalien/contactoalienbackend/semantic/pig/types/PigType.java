package com.usac.contactoalien.contactoalienbackend.semantic.pig.types;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;

import java.util.Objects;

// Represents a Pig Latin type: a scalar (numerus/decimalis/textum/littera/bool), a series
// (array) of some element type, a structure instance (referring to an imported Y? structure by
// name), an object instance (referring to an imported Zetariano class by name), or an
// unknown/error placeholder used to suppress cascading errors after a previous mistake.
public final class PigType {

    public enum Kind { NUMERUS, DECIMALIS, TEXTUM, LITTERA, BOOL, ARRAY, STRUCT, OBJECT, UNKNOWN }

    public static final PigType NUMERUS = new PigType(Kind.NUMERUS, null, null, null);
    public static final PigType DECIMALIS = new PigType(Kind.DECIMALIS, null, null, null);
    public static final PigType TEXTUM = new PigType(Kind.TEXTUM, null, null, null);
    public static final PigType LITTERA = new PigType(Kind.LITTERA, null, null, null);
    public static final PigType BOOL = new PigType(Kind.BOOL, null, null, null);
    public static final PigType UNKNOWN = new PigType(Kind.UNKNOWN, null, null, null);

    private final Kind kind;
    private final PigType elementType;
    private final String structName;
    private final String className;

    private PigType(Kind kind, PigType elementType, String structName, String className) {
        this.kind = kind;
        this.elementType = elementType;
        this.structName = structName;
        this.className = className;
    }

    public static PigType ofArray(PigType elementType) { return new PigType(Kind.ARRAY, elementType, null, null); }
    public static PigType ofStruct(String name) { return new PigType(Kind.STRUCT, null, name, null); }
    public static PigType ofObject(String name) { return new PigType(Kind.OBJECT, null, null, name); }

    public Kind getKind() { return kind; }
    public PigType getElementType() { return elementType; }
    public String getStructName() { return structName; }
    public String getClassName() { return className; }
    public boolean isUnknown() { return kind == Kind.UNKNOWN; }
    public boolean isNumeric() { return kind == Kind.NUMERUS || kind == Kind.DECIMALIS; }

    // Maps a Zetariano method/field return type to its Pig Latin equivalent, per the project's
    // type table. Zetariano array types and 'null'/'void' have no defined Pig Latin counterpart
    // in this scope, so they collapse to UNKNOWN to suppress cascading errors.
    public static PigType fromZetariano(ZType zType) {
        if (zType.isArray()) return UNKNOWN;
        return switch (zType.getKind()) {
            case INT -> NUMERUS;
            case DOUBLE -> DECIMALIS;
            case CHAR -> LITTERA;
            case BOOLEAN -> BOOL;
            case STRING -> TEXTUM;
            case CLASS -> ofObject(zType.getClassName());
            default -> UNKNOWN; // VOID, NULL, UNKNOWN
        };
    }

    // Maps a Y? function/field return type to its Pig Latin equivalent, per the project's type
    // table. Y? array types and 'void' have no defined Pig Latin counterpart in this scope, so
    // they collapse to UNKNOWN.
    public static PigType fromY(YType yType) {
        if (yType.isArray()) return UNKNOWN;
        return switch (yType.getKind()) {
            case ENTERO -> NUMERUS;
            case FLOTANTE -> DECIMALIS;
            case CARACTER -> LITTERA;
            case BOOL -> BOOL;
            case CADENA -> TEXTUM;
            case STRUCT -> ofStruct(yType.getStructName());
            default -> UNKNOWN; // VOID, UNKNOWN
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PigType other)) return false;
        return kind == other.kind
                && Objects.equals(structName, other.structName)
                && Objects.equals(className, other.className)
                && Objects.equals(elementType, other.elementType);
    }

    @Override
    public int hashCode() { return Objects.hash(kind, structName, className, elementType); }

    @Override
    public String toString() {
        return switch (kind) {
            case ARRAY -> "series[" + elementType + "]";
            case STRUCT -> structName;
            case OBJECT -> className;
            case UNKNOWN -> "<unknown>";
            default -> kind.name().toLowerCase();
        };
    }
}