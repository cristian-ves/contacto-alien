package com.usac.contactoalien.contactoalienbackend.codegen.quad;

/**
Operations of the quadruple intermediate representation. Every value is a 64-bit word
(integer, character, boolean, string pointer or object pointer). New operations are added
here and given a case in Quad.format and in the RISC-V QuadTranslator.
*/
public enum QuadOp {
    // Control flow: the target label is always held in `result`.
    LABEL, GOTO, IF_FALSE, IF_TRUE,

    // Data movement and unary operations.
    ASSIGN, NEG, NOT,

    // Binary operations; the symbol is only used to display the quadruple.
    ADD("+"), SUB("-"), MUL("*"), DIV("/"), MOD("%"),
    LT("<"), GT(">"), LE("<="), GE(">="), EQ("=="), NE("!="),
    AND("&&"), OR("||"),

    // Decimal (double) arithmetic and comparisons, displayed with a trailing dot; I2F and F2I convert
    // between integer and decimal.
    FADD("+."), FSUB("-."), FMUL("*."), FDIV("/."), FMOD("%."),
    FLT("<."), FGT(">."), FLE("<=."), FGE(">=."), FEQ("==."), FNE("!=."),
    FNEG, I2F, F2I,

    // Functions: FUNC_BEGIN/FUNC_END carry the label in `result`, RECV names a formal parameter
    // (in order), PARAM pushes an argument, CALL is (label, argument count, result).
    FUNC_BEGIN, FUNC_END, RECV, PARAM, CALL, RETURN,

    // Objects on the heap: NEW is (size in bytes, class id, result); fields are word offsets in bytes.
    NEW, LOAD_FIELD, STORE_FIELD,

    // NULL_CHECK (reference) stops the program with "Null reference" when the reference is null; it guards calls
    // of methods that go straight to a label, which would otherwise never touch the object.
    NULL_CHECK,

    // Dynamic dispatch: VTABLE (class id, slot, label) declares one entry of a class's method table;
    // LOAD_METHOD (receiver, slot, result) fetches the address of that slot for the receiver's class;
    // CALL_INDIRECT (address, argument count, result) calls it like CALL.
    VTABLE, LOAD_METHOD, CALL_INDIRECT,

    // Arrays on the heap: NEW_ARRAY is (length, _, result); LOAD_INDEX is (array, index, result) and
    // STORE_INDEX is (value, array, index) with bounds checking at run time.
    NEW_ARRAY, LOAD_INDEX, STORE_INDEX,

    // Strings; STR_EQ compares contents.
    STR_CONCAT, INT_TO_STR, CHAR_TO_STR, BOOL_TO_STR, FLOAT_TO_STR, STR_EQ("=s="),

    // Output.
    PRINT_STR, PRINT_INT, PRINT_CHAR, PRINT_BOOL, PRINT_FLOAT, PRINT_NL,

    // Input: each reads one line of standard input and yields it as a string, an integer, a
    // decimal or the first character.
    READ_STR, READ_INT, READ_FLOAT, READ_CHAR;

    private final String symbol;

    QuadOp() {
        this(null);
    }

    QuadOp(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public boolean isBinary() {
        return symbol != null;
    }
}
