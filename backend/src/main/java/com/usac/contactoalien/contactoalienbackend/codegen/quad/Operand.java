package com.usac.contactoalien.contactoalienbackend.codegen.quad;

/**
Conventions for quadruple operands, which are plain strings: an integer literal is written
as digits (optionally negative), a string literal is its decoded text between double quotes,
a decimal literal starts with '#' and holds the number (it is loaded as the bits of a double), a global
variable starts with '@', and anything else is a local variable or a temporary.
*/
public final class Operand {

    private static final String GLOBAL_PREFIX = "@";
    private static final String FLOAT_PREFIX = "#";

    private Operand() {
    }

    public static boolean isIntLiteral(String operand) {
        if (operand == null || operand.isEmpty()) {
            return false;
        }
        int start = operand.charAt(0) == '-' ? 1 : 0;
        if (start == operand.length()) {
            return false;
        }
        for (int i = start; i < operand.length(); i++) {
            if (!Character.isDigit(operand.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStringLiteral(String operand) {
        return operand != null && operand.length() >= 2 && operand.charAt(0) == '"';
    }

    public static String stringLiteral(String decodedText) {
        return "\"" + decodedText + "\"";
    }

    public static String stringValue(String literal) {
        return literal.substring(1, literal.length() - 1);
    }

    public static boolean isFloatLiteral(String operand) {
        return operand != null && operand.startsWith(FLOAT_PREFIX);
    }

    public static String floatLiteral(double value) {
        return FLOAT_PREFIX + value;
    }

    // The 64 bits of the IEEE double a decimal literal stands for.
    public static long floatBits(String literal) {
        return Double.doubleToRawLongBits(Double.parseDouble(literal.substring(FLOAT_PREFIX.length())));
    }

    public static boolean isGlobal(String operand) {
        return operand != null && operand.startsWith(GLOBAL_PREFIX);
    }

    public static String global(String name) {
        return GLOBAL_PREFIX + name;
    }

    // True for operands that live in a stack slot or a global, false for literals.
    public static boolean isVariable(String operand) {
        return operand != null && !isIntLiteral(operand) && !isStringLiteral(operand) && !isFloatLiteral(operand);
    }

    // Makes control characters visible when a quadruple is printed.
    public static String show(String operand) {
        if (operand == null) {
            return "_";
        }
        if (!isStringLiteral(operand)) {
            return operand;
        }
        return operand.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
