package com.usac.contactoalien.contactoalienbackend.semantic.pig.types;

import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;

import java.util.Objects;

// Represents a Pig Latin type: a scalar (numerus/decimalis/textum/littera/bool), a series
// (array) of some element type, a structure instance (referring to an imported Y? structure by
// name), an object instance (referring to an imported Zetariano class by name), or an
// unknown/error placeholder used to suppress cascading errors after a previous mistake.
public final class PigType {

    public enum Kind { NUMERUS, DECIMALIS, TEXTUM, LITTERA, BOOL, ARRAY, STRUCT, OBJECT, NULL, VOID, UNKNOWN }

    public static final PigType NUMERUS = new PigType(Kind.NUMERUS, null, null, null);
    public static final PigType DECIMALIS = new PigType(Kind.DECIMALIS, null, null, null);
    public static final PigType TEXTUM = new PigType(Kind.TEXTUM, null, null, null);
    public static final PigType LITTERA = new PigType(Kind.LITTERA, null, null, null);
    public static final PigType BOOL = new PigType(Kind.BOOL, null, null, null);
    // The type of the literal `null`, which fits any object variable.
    public static final PigType NULL = new PigType(Kind.NULL, null, null, null);
    // The result of a function or method that returns nothing; it cannot be used as a value.
    public static final PigType VOID = new PigType(Kind.VOID, null, null, null);
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
    // littera takes part in arithmetic and comparisons as the lowest numeric level of the hierarchy.
    public boolean isArithmetic() { return isNumeric() || kind == Kind.LITTERA; }

    // The Zetariano type of the same value, used to check the arguments of a method or constructor call.
    // Structures have no Zetariano counterpart, so they become UNKNOWN and are accepted anywhere.
    public ZType toZetariano() {
        return switch (kind) {
            case NUMERUS -> ZType.INT;
            case DECIMALIS -> ZType.DOUBLE;
            case LITTERA -> ZType.CHAR;
            case BOOL -> ZType.BOOLEAN;
            case TEXTUM -> ZType.STRING;
            case OBJECT -> ZType.ofClass(className);
            case NULL -> ZType.NULL;
            case VOID -> ZType.VOID;
            case ARRAY -> elementType.toZetariano().isUnknown() ? ZType.UNKNOWN : elementType.toZetariano().asArray(1);
            default -> ZType.UNKNOWN;
        };
    }

    // Maps a Zetariano method/field return type to its Pig Latin equivalent, per the project's
    // type table. Arrays become series of the mapped element type; 'null'/'void' have no defined
    // Pig Latin counterpart in this scope, so null collapses to UNKNOWN to suppress cascading errors.
    public static PigType fromZetariano(ZType zType) {
        if (zType.isArray()) {
            PigType element = fromZetariano(zType.elementType());
            return element.isUnknown() ? UNKNOWN : ofArray(element);
        }
        return switch (zType.getKind()) {
            case INT -> NUMERUS;
            case DOUBLE -> DECIMALIS;
            case CHAR -> LITTERA;
            case BOOLEAN -> BOOL;
            case STRING -> TEXTUM;
            case CLASS -> ofObject(zType.getClassName());
            case VOID -> VOID;
            default -> UNKNOWN; // NULL, UNKNOWN
        };
    }

    // Maps a Y? function/field return type to its Pig Latin equivalent, per the project's type
    // table. Arrays become series of the mapped element type.
    public static PigType fromY(YType yType) {
        if (yType.isArray()) {
            PigType element = fromY(yType.elementType());
            return element.isUnknown() ? UNKNOWN : ofArray(element);
        }
        return switch (yType.getKind()) {
            case ENTERO -> NUMERUS;
            case FLOTANTE -> DECIMALIS;
            case CARACTER -> LITTERA;
            case BOOL -> BOOL;
            case CADENA -> TEXTUM;
            case STRUCT -> ofStruct(yType.getStructName());
            case VOID -> VOID;
            default -> UNKNOWN; // UNKNOWN
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
            case NULL -> "null";
            case UNKNOWN -> "<unknown>";
            default -> kind.name().toLowerCase();
        };
    }
}