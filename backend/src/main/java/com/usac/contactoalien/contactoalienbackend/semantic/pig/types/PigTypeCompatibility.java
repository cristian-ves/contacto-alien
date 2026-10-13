package com.usac.contactoalien.contactoalienbackend.semantic.pig.types;

import java.util.Objects;

// Same compatibility rules as Zetariano's TypeCompatibility / Y?'s YTypeCompatibility, adapted
// to PigType, plus a rule specific to Pig Latin: relational and equality expressions always
// type as BOOL, needed to validate si/dum/facere/per conditions.
public final class PigTypeCompatibility {

    private PigTypeCompatibility() {}

    public static boolean isAssignable(PigType target, PigType source) {
        if (target.isUnknown() || source.isUnknown()) return true;
        return switch (target.getKind()) {
            case NUMERUS -> source.getKind() == PigType.Kind.NUMERUS;
            case DECIMALIS -> source.getKind() == PigType.Kind.DECIMALIS || source.getKind() == PigType.Kind.NUMERUS;
            case LITTERA -> source.getKind() == PigType.Kind.LITTERA;
            case BOOL -> source.getKind() == PigType.Kind.BOOL;
            case TEXTUM -> source.getKind() == PigType.Kind.TEXTUM;
            case ARRAY -> source.getKind() == PigType.Kind.ARRAY
                    && Objects.equals(target.getElementType(), source.getElementType());
            case STRUCT -> source.getKind() == PigType.Kind.STRUCT
                    && Objects.equals(target.getStructName(), source.getStructName());
            case OBJECT -> source.getKind() == PigType.Kind.OBJECT
                    && Objects.equals(target.getClassName(), source.getClassName());
            default -> false;
        };
    }

    public static PigType arithmeticResult(PigType left, PigType right) {
        if (left.getKind() == PigType.Kind.DECIMALIS || right.getKind() == PigType.Kind.DECIMALIS) return PigType.DECIMALIS;
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
        return left.isNumeric() && right.isNumeric();
    }

    public static boolean isValidEqualityPair(PigType left, PigType right) {
        if (left.isUnknown() || right.isUnknown()) return true;
        if (left.isNumeric() && right.isNumeric()) return true;
        if (left.getKind() == PigType.Kind.BOOL && right.getKind() == PigType.Kind.BOOL) return true;
        if (left.getKind() == PigType.Kind.LITTERA && right.getKind() == PigType.Kind.LITTERA) return true;
        if (left.getKind() == PigType.Kind.TEXTUM && right.getKind() == PigType.Kind.TEXTUM) return true;
        if (left.getKind() == PigType.Kind.STRUCT && right.getKind() == PigType.Kind.STRUCT) {
            return Objects.equals(left.getStructName(), right.getStructName());
        }
        if (left.getKind() == PigType.Kind.OBJECT && right.getKind() == PigType.Kind.OBJECT) {
            return Objects.equals(left.getClassName(), right.getClassName());
        }
        return false;
    }
}