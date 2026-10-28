package com.usac.contactoalien.contactoalienbackend.semantic.types;

import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;

// Central table of type-compatibility rules for Zetariano. Each rule is documented with a
// one-line comment, since this class feeds directly into the type-compatibility table of the
// project's technical documentation.
public final class TypeCompatibility {

    private TypeCompatibility() {}

    // Assignment compatibility: can a value of 'source' type be assigned to a variable of 'target' type?
    public static boolean isAssignable(ZType target, ZType source) {
        return isAssignable(target, source, null);
    }

    // With a registry, an instance of a subclass can also be assigned where its superclass is expected.
    public static boolean isAssignable(ZType target, ZType source, CompilationUnitRegistry registry) {
        if (target.isUnknown() || source.isUnknown()) return true; // suppress cascading errors
        // null is a valid value for any reference: String, array or class
        if (source.getKind() == ZType.Kind.NULL && !source.isArray()) {
            return target.isArray() || target.getKind() == ZType.Kind.STRING || target.getKind() == ZType.Kind.CLASS;
        }
        if (target.isArray() || source.isArray()) {
            // Arrays: only exact same base kind/class and same dimension count are compatible (no widening across arrays)
            return target.getKind() == source.getKind()
                    && target.getArrayDimensions() == source.getArrayDimensions()
                    && java.util.Objects.equals(target.getClassName(), source.getClassName());
        }
        return switch (target.getKind()) {
            // int accepts int or char (widening, as in Java); no implicit narrowing from double
            case INT -> source.getKind() == ZType.Kind.INT || source.getKind() == ZType.Kind.CHAR;
            // double accepts double, int or char (widening, same rule as Java)
            case DOUBLE -> source.getKind() == ZType.Kind.DOUBLE || source.getKind() == ZType.Kind.INT
                    || source.getKind() == ZType.Kind.CHAR;
            // char is a separate type: no implicit int<->char conversion
            case CHAR -> source.getKind() == ZType.Kind.CHAR;
            // boolean only accepts boolean
            case BOOLEAN -> source.getKind() == ZType.Kind.BOOLEAN;
            // String assignment requires an actual String; '+' concatenation-to-text only applies inside '+' expressions, not on plain assignment
            case STRING -> source.getKind() == ZType.Kind.STRING;
            // a class-typed variable accepts an instance of the same class, of a subclass, or null
            case CLASS -> source.getKind() == ZType.Kind.NULL
                    || (source.getKind() == ZType.Kind.CLASS && isSameOrSubclass(source, target, registry));
            default -> false;
        };
    }

    // Result type of an arithmetic operator (+,-,*,/,%) on two numeric operands: double if either operand is double, otherwise int (same rule as Java)
    public static ZType arithmeticResult(ZType left, ZType right) {
        if (left.getKind() == ZType.Kind.DOUBLE || right.getKind() == ZType.Kind.DOUBLE) return ZType.DOUBLE;
        return ZType.INT;
    }

    // '+' with at least one String operand concatenates any other type (int, double, char, boolean) converted to text; result is always String
    public static boolean isStringConcatenation(ZType left, ZType right) {
        return left.getKind() == ZType.Kind.STRING || right.getKind() == ZType.Kind.STRING;
    }

    // Logical operators (&&, ||, !) only accept boolean operands
    public static boolean isValidLogicalOperand(ZType type) {
        if (type.isUnknown()) return true;
        return type.getKind() == ZType.Kind.BOOLEAN;
    }

    // Relational operators (<, >, <=, >=) only accept numeric (int/double) operands on both sides
    public static boolean isValidRelationalPair(ZType left, ZType right) {
        if (left.isUnknown() || right.isUnknown()) return true;
        return left.isNumeric() && right.isNumeric();
    }

    // Equality operators (==, !=) accept: numeric<->numeric, boolean<->boolean, char<->char,
    // String<->String (reference comparison), class-reference<->class-reference of the same
    // class, or class-reference<->null
    public static boolean isValidEqualityPair(ZType left, ZType right) {
        return isValidEqualityPair(left, right, null);
    }

    // With a registry, two references are comparable when one class is the same as or a subclass of the other.
    public static boolean isValidEqualityPair(ZType left, ZType right, CompilationUnitRegistry registry) {
        if (left.isUnknown() || right.isUnknown()) return true;
        if (left.isNumeric() && right.isNumeric()) return true;
        if (left.getKind() == ZType.Kind.BOOLEAN && right.getKind() == ZType.Kind.BOOLEAN) return true;
        if (left.getKind() == ZType.Kind.CHAR && right.getKind() == ZType.Kind.CHAR) return true;
        if (left.getKind() == ZType.Kind.STRING && right.getKind() == ZType.Kind.STRING) return true;
        // a String or an array can be compared with null (but not with each other or with a class)
        if (left.getKind() == ZType.Kind.NULL && (right.getKind() == ZType.Kind.STRING || right.isArray())) return true;
        if (right.getKind() == ZType.Kind.NULL && (left.getKind() == ZType.Kind.STRING || left.isArray())) return true;
        if (left.isArray() || right.isArray()) {
            return left.isArray() && right.isArray() && left.equals(right);
        }
        boolean leftIsClassOrNull = left.getKind() == ZType.Kind.CLASS || left.getKind() == ZType.Kind.NULL;
        boolean rightIsClassOrNull = right.getKind() == ZType.Kind.CLASS || right.getKind() == ZType.Kind.NULL;
        if (leftIsClassOrNull && rightIsClassOrNull) {
            if (left.getKind() == ZType.Kind.NULL || right.getKind() == ZType.Kind.NULL) return true;
            return isSameOrSubclass(left, right, registry) || isSameOrSubclass(right, left, registry);
        }
        return false;
    }

    private static boolean isSameOrSubclass(ZType sub, ZType sup, CompilationUnitRegistry registry) {
        if (sub.getClassName().equals(sup.getClassName())) return true;
        return registry != null && registry.isSubclass(sub.getClassName(), sup.getClassName());
    }
}