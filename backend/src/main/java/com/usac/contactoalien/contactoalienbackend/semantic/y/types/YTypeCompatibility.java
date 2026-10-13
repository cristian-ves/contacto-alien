package com.usac.contactoalien.contactoalienbackend.semantic.y.types;

import java.util.Objects;

// Same compatibility rules as Zetariano's TypeCompatibility, renamed to Y?'s type names, plus
// Structure<->Structure equality (Y? has no 'null', so there is no reference/null case here).
public final class YTypeCompatibility {

    private YTypeCompatibility() {}

    // Assignment compatibility: can a value of 'source' type be assigned to a variable of 'target' type?
    public static boolean isAssignable(YType target, YType source) {
        if (target.isUnknown() || source.isUnknown()) return true;
        if (target.isArray() || source.isArray()) {
            return target.getKind() == source.getKind()
                    && target.getArrayDimensions() == source.getArrayDimensions()
                    && Objects.equals(target.getStructName(), source.getStructName());
        }
        return switch (target.getKind()) {
            // entero only accepts entero; no implicit narrowing from flotante
            case ENTERO -> source.getKind() == YType.Kind.ENTERO;
            // flotante accepts flotante or entero (widening, same rule as Java)
            case FLOTANTE -> source.getKind() == YType.Kind.FLOTANTE || source.getKind() == YType.Kind.ENTERO;
            // caracter is a separate type: no implicit entero<->caracter conversion
            case CARACTER -> source.getKind() == YType.Kind.CARACTER;
            // bool only accepts bool
            case BOOL -> source.getKind() == YType.Kind.BOOL;
            // cadena assignment requires an actual cadena value; text-conversion only applies inside '+'
            case CADENA -> source.getKind() == YType.Kind.CADENA;
            // a structure-typed variable accepts an instance of the same structure
            case STRUCT -> source.getKind() == YType.Kind.STRUCT && target.getStructName().equals(source.getStructName());
            default -> false;
        };
    }

    // Result type of an arithmetic operator (+,-,*,/) on two numeric operands: flotante if either operand is flotante, otherwise entero
    public static YType arithmeticResult(YType left, YType right) {
        if (left.getKind() == YType.Kind.FLOTANTE || right.getKind() == YType.Kind.FLOTANTE) return YType.FLOTANTE;
        return YType.ENTERO;
    }

    // '+' with at least one cadena operand concatenates any other type converted to text; result is always cadena
    public static boolean isStringConcatenation(YType left, YType right) {
        return left.getKind() == YType.Kind.CADENA || right.getKind() == YType.Kind.CADENA;
    }

    // Logical operators (&&, ||, !) only accept bool operands
    public static boolean isValidLogicalOperand(YType type) {
        if (type.isUnknown()) return true;
        return type.getKind() == YType.Kind.BOOL;
    }

    // Relational operators (<, >, <=, >=) only accept numeric (entero/flotante) operands
    public static boolean isValidRelationalPair(YType left, YType right) {
        if (left.isUnknown() || right.isUnknown()) return true;
        return left.isNumeric() && right.isNumeric();
    }

    // Equality operators (==, !=) accept: numeric<->numeric, bool<->bool, caracter<->caracter,
    // cadena<->cadena, and Structure<->Structure of the same type
    public static boolean isValidEqualityPair(YType left, YType right) {
        if (left.isUnknown() || right.isUnknown()) return true;
        if (left.isNumeric() && right.isNumeric()) return true;
        if (left.getKind() == YType.Kind.BOOL && right.getKind() == YType.Kind.BOOL) return true;
        if (left.getKind() == YType.Kind.CARACTER && right.getKind() == YType.Kind.CARACTER) return true;
        if (left.getKind() == YType.Kind.CADENA && right.getKind() == YType.Kind.CADENA) return true;
        if (left.getKind() == YType.Kind.STRUCT && right.getKind() == YType.Kind.STRUCT) {
            return Objects.equals(left.getStructName(), right.getStructName());
        }
        return false;
    }
}