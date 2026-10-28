package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;

/**
Static type of a generated value. The generators only need the kind to choose between
integer arithmetic, string concatenation, printing routines and so on; className is set
for object references and element for arrays. A decimal value is the 64 bits of an IEEE double
held in one word, like every other value.
*/
public record GenType(Kind kind, String className, GenType element) {

    public enum Kind { INT, CHAR, BOOL, STRING, FLOAT, VOID, NULL, OBJECT, ARRAY }

    public GenType(Kind kind, String className) {
        this(kind, className, null);
    }

    public static final GenType INT = new GenType(Kind.INT, null);
    public static final GenType CHAR = new GenType(Kind.CHAR, null);
    public static final GenType BOOL = new GenType(Kind.BOOL, null);
    public static final GenType STRING = new GenType(Kind.STRING, null);
    public static final GenType FLOAT = new GenType(Kind.FLOAT, null);
    public static final GenType VOID = new GenType(Kind.VOID, null);
    public static final GenType NULL = new GenType(Kind.NULL, null);

    public static GenType object(String className) {
        return new GenType(Kind.OBJECT, className);
    }

    public static GenType array(GenType element) {
        return new GenType(Kind.ARRAY, null, element);
    }

    public boolean isNumeric() {
        return kind == Kind.INT || kind == Kind.CHAR || kind == Kind.FLOAT;
    }

    public boolean isFloat() {
        return kind == Kind.FLOAT;
    }

    public boolean isReference() {
        return kind == Kind.OBJECT || kind == Kind.NULL || kind == Kind.STRING || kind == Kind.ARRAY;
    }

    /**
    Cost of passing a value of this type where target is expected, used to choose between
    overloads: 0 for the same type, 1 or 2 for a widening (int or char to decimal, char to int,
    null to any reference) and -1 when the value is not accepted at all.
    */
    public int conversionCost(GenType target) {
        if (equals(target)) {
            return 0;
        }
        if (target.isFloat() && (kind == Kind.INT || kind == Kind.CHAR)) {
            return kind == Kind.INT ? 1 : 2;
        }
        if (target.kind() == Kind.INT && kind == Kind.CHAR) {
            return 1;
        }
        return kind == Kind.NULL && target.isReference() ? 1 : -1;
    }

    // Value of a variable declared without an initializer: 0, false, null or the empty string.
    public String defaultOperand() {
        return switch (kind) {
            case STRING -> Operand.stringLiteral("");
            case FLOAT -> Operand.floatLiteral(0.0);
            default -> "0";
        };
    }

    // One-letter code used to tell overloaded methods apart in their assembly labels.
    public String signatureCode() {
        return switch (kind) {
            case INT -> "I";
            case CHAR -> "C";
            case BOOL -> "B";
            case STRING -> "S";
            case FLOAT -> "F";
            case ARRAY -> "A" + element.signatureCode();
            case VOID -> "V";
            case NULL -> "N";
            case OBJECT -> "O" + className;
        };
    }
}
