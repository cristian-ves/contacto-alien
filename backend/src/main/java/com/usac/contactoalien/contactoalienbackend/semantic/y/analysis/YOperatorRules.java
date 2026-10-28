package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeCompatibility;
import org.antlr.v4.runtime.ParserRuleContext;

// Result types of the binary arithmetic operators of Y?, with the error for operands they do not accept.
final class YOperatorRules {

    private YOperatorRules() {
    }

    // A caracter takes part in arithmetic through its code, as in Pig Latin and Zetariano; the result is an entero.
    private static boolean takesArithmetic(YType type) {
        return type.isNumeric() || (!type.isArray() && type.getKind() == YType.Kind.CARACTER);
    }

    // Arrays and void values have no text form; structures do (StructFormatter).
    static void requirePrintable(YErrorReporter errors, YType type, ParserRuleContext ctx) {
        if (type.isArray() || type.getKind() == YType.Kind.VOID) {
            errors.report("Cannot print a value of type '" + type + "'; print its elements one by one", ctx);
        }
    }

    static YType plus(YErrorReporter errors, YType left, YType right, ParserRuleContext ctx) {
        if (left.isUnknown() || right.isUnknown()) return YType.UNKNOWN;
        if (YTypeCompatibility.isStringConcatenation(left, right)) {
            YType other = left.getKind() == YType.Kind.CADENA && !left.isArray() ? right : left;
            if (other.isArray() || other.getKind() == YType.Kind.VOID) {
                errors.report("Cannot concatenate a value of type '" + other + "' to a cadena; use its elements one by one", ctx);
            }
            return YType.CADENA;
        }
        if (takesArithmetic(left) && takesArithmetic(right)) return YTypeCompatibility.arithmeticResult(left, right);
        errors.report("Operator '+' requires two numeric operands or at least one cadena operand, found '" + left + "' and '" + right + "'", ctx);
        return YType.UNKNOWN;
    }

    static YType arithmetic(YErrorReporter errors, YType left, YType right, String opSymbol, ParserRuleContext ctx) {
        if (left.isUnknown() || right.isUnknown()) return YType.UNKNOWN;
        if (!(takesArithmetic(left) && takesArithmetic(right))) {
            errors.report("Operator '" + opSymbol + "' requires numeric operands, found '" + left + "' and '" + right + "'", ctx);
            return YType.UNKNOWN;
        }
        YType result = YTypeCompatibility.arithmeticResult(left, right);
        if (opSymbol.equals("%") && result.getKind() == YType.Kind.FLOTANTE) {
            errors.report("Operator '%' requires integer operands, found '" + left + "' and '" + right + "'", ctx);
            return YType.UNKNOWN;
        }
        return result;
    }
}
