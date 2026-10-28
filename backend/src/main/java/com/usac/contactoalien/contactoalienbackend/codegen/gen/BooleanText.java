package com.usac.contactoalien.contactoalienbackend.codegen.gen;

/**
How a language writes its boolean values when they are printed or concatenated. Each language
uses the words of its own literals; LATIN is served by the runtime constants.
*/
public enum BooleanText {
    LATIN(null, null),
    JAVA("true", "false"),
    SPANISH("verdadero", "falso");

    private final String trueText;
    private final String falseText;

    BooleanText(String trueText, String falseText) {
        this.trueText = trueText;
        this.falseText = falseText;
    }

    boolean fromRuntime() {
        return this == LATIN;
    }

    String trueText() {
        return trueText;
    }

    String falseText() {
        return falseText;
    }
}
