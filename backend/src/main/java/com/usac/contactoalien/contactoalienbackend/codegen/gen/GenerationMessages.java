package com.usac.contactoalien.contactoalienbackend.codegen.gen;

// Every message the code generation phase can report, kept in one place.
public final class GenerationMessages {

    public static final String ONLY_PIG = "Only Pig Latin programs can be compiled to RISC-V";
    public static final String INTERNAL = "Internal error while generating code";
    public static final String MISSING_IMPORT = "Imported file was not provided: ";
    public static final String UNKNOWN_FUNCTION = "Unknown function: ";
    public static final String UNKNOWN_CLASS = "Unknown class: ";
    public static final String UNKNOWN_MEMBER = "Unknown member: ";
    public static final String UNKNOWN_VARIABLE = "Unknown variable: ";

    public static final String FEATURE_DECIMALS = "Decimal numbers in Zetariano";
    public static final String FEATURE_READ = "Reading input into this kind of variable";
    public static final String FEATURE_ARRAYS = "Arrays";
    public static final String FEATURE_STRUCTS = "Structures";
    public static final String FEATURE_OBJECT_PRINT = "Printing objects";
    public static final String FEATURE_INHERITANCE = "Inheritance";
    public static final String FEATURE_SWITCH = "Multiple selection";
    public static final String FEATURE_ASSIGN_TARGET = "This assignment target";
    public static final String FEATURE_EXPRESSION = "This expression";

    private GenerationMessages() {
    }

    public static String notSupported(String feature) {
        return feature + " is not supported yet";
    }
}
