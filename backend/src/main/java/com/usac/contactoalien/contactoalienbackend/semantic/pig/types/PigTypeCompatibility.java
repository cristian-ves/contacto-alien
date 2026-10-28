package com.usac.contactoalien.contactoalienbackend.semantic.pig.types;

import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;

import java.util.Objects;

// Same compatibility rules as Zetariano's TypeCompatibility / Y?'s YTypeCompatibility, adapted
// to PigType, plus a rule specific to Pig Latin: relational and equality expressions always
// type as BOOL, needed to validate si/dum/facere/per conditions.
public final class PigTypeCompatibility {

    private PigTypeCompatibility() {}

    public static boolean isAssignable(PigType target, PigType source) {
        return isAssignable(target, source, null);
    }

    // With a registry, an object of a subclass fits a variable of its superclass; null fits any object variable.
    public static boolean isAssignable(PigType target, PigType source, CompilationUnitRegistry registry) {
        if (target.isUnknown() || source.isUnknown()) return true;
        return switch (target.getKind()) {
            // a littera widens to a number through its code
            case NUMERUS -> source.getKind() == PigType.Kind.NUMERUS || source.getKind() == PigType.Kind.LITTERA;
            case DECIMALIS -> source.getKind() == PigType.Kind.DECIMALIS || source.getKind() == PigType.Kind.NUMERUS
                    || source.getKind() == PigType.Kind.LITTERA;
            case LITTERA -> source.getKind() == PigType.Kind.LITTERA;
            case BOOL -> source.getKind() == PigType.Kind.BOOL;
            case TEXTUM -> source.getKind() == PigType.Kind.TEXTUM;
            case ARRAY -> source.getKind() == PigType.Kind.ARRAY
                    && Objects.equals(target.getElementType(), source.getElementType());
            case STRUCT -> source.getKind() == PigType.Kind.STRUCT
                    && Objects.equals(target.getStructName(), source.getStructName());
            case OBJECT -> source.getKind() == PigType.Kind.NULL
                    || (source.getKind() == PigType.Kind.OBJECT && isSameOrSubclass(source, target, registry));
            default -> false;
        };
    }

    public static PigType arithmeticResult(PigType left, PigType right) {
        if (left.getKind() == PigType.Kind.DECIMALIS || right.getKind() == PigType.Kind.DECIMALIS) return PigType.DECIMALIS;
        if (left.getKind() == PigType.Kind.LITTERA && right.getKind() == PigType.Kind.LITTERA) return PigType.LITTERA;
        return PigType.NUMERUS;
    }

    public static boolean isStringConcatenation(PigType left, PigType right) {
        return left.getKind() == PigType.Kind.TEXTUM || right.getKind() == PigType.Kind.TEXTUM;
    }

    public static boolean isValidLogicalOperand(PigType type) {
        if (type.isUnknown()) return true;
        return type.getKind() == PigType.Kind.BOOL;
    }

    public static boolean isValidRelationalPair(PigType left, PigType right) {
        if (left.isUnknown() || right.isUnknown()) return true;
        return left.isArithmetic() && right.isArithmetic();
    }

    public static boolean isValidEqualityPair(PigType left, PigType right) {
        return isValidEqualityPair(left, right, null);
    }

    public static boolean isValidEqualityPair(PigType left, PigType right, CompilationUnitRegistry registry) {
        if (left.isUnknown() || right.isUnknown()) return true;
        if (left.isArithmetic() && right.isArithmetic()) return true;
        if (left.getKind() == PigType.Kind.BOOL && right.getKind() == PigType.Kind.BOOL) return true;
        if (left.getKind() == PigType.Kind.TEXTUM && right.getKind() == PigType.Kind.TEXTUM) return true;
        if (left.getKind() == PigType.Kind.STRUCT && right.getKind() == PigType.Kind.STRUCT) {
            return Objects.equals(left.getStructName(), right.getStructName());
        }
        boolean leftNullable = left.getKind() == PigType.Kind.OBJECT || left.getKind() == PigType.Kind.NULL;
        boolean rightNullable = right.getKind() == PigType.Kind.OBJECT || right.getKind() == PigType.Kind.NULL;
        if (leftNullable && rightNullable) {
            if (left.getKind() == PigType.Kind.NULL || right.getKind() == PigType.Kind.NULL) return true;
            return isSameOrSubclass(left, right, registry) || isSameOrSubclass(right, left, registry);
        }
        return false;
    }

    private static boolean isSameOrSubclass(PigType sub, PigType sup, CompilationUnitRegistry registry) {
        if (Objects.equals(sub.getClassName(), sup.getClassName())) return true;
        return registry != null && registry.isSubclass(sub.getClassName(), sup.getClassName());
    }
}