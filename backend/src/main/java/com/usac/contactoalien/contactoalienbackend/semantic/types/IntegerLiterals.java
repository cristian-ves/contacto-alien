package com.usac.contactoalien.contactoalienbackend.semantic.types;

/**
Integer literals are 64-bit values; one that does not fit is rejected by the analyzers so that the
code generator, which stores them in a word, never meets it.
*/
public final class IntegerLiterals {

    private IntegerLiterals() {
    }

    public static boolean fits(String tokenText) {
        try {
            Long.parseLong(tokenText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String outOfRange(String tokenText) {
        return "Integer literal out of range: " + tokenText;
    }

    // The declared size of an array; a size beyond the range of an int is reported by the caller and counts as the largest int.
    public static int length(String tokenText) {
        try {
            return Integer.parseInt(tokenText);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public static boolean isLengthTooLarge(String tokenText) {
        return length(tokenText) == Integer.MAX_VALUE && !tokenText.equals(Integer.toString(Integer.MAX_VALUE));
    }

    public static String lengthOutOfRange(String tokenText) {
        return "Array size out of range: " + tokenText;
    }
}
