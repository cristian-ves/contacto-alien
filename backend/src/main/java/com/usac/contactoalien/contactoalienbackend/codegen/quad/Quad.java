package com.usac.contactoalien.contactoalienbackend.codegen.quad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
One quadruple (op, arg1, arg2, result). Unused slots are null. See QuadOp for what each
operation keeps in each slot.
*/
public record Quad(QuadOp op, String arg1, String arg2, String result) {

    // Text form of the quadruple, close to the three-address notation of the project statement.
    @JsonProperty("text")
    public String format() {
        String a = Operand.show(arg1);
        String b = Operand.show(arg2);
        return switch (op) {
            case LABEL -> result + ":";
            case GOTO -> "goto " + result;
            case IF_FALSE -> "if_false " + a + " goto " + result;
            case IF_TRUE -> "if_true " + a + " goto " + result;
            case ASSIGN -> result + " = " + a;
            case NEG -> result + " = -" + a;
            case NOT -> result + " = !" + a;
            case FNEG -> result + " = -." + a;
            case I2F -> result + " = i2f " + a;
            case F2I -> result + " = f2i " + a;
            case FUNC_BEGIN -> "func " + result + ":";
            case FUNC_END -> "end " + result;
            case RECV -> "recv " + result;
            case PARAM -> "param " + a;
            case CALL -> (result == null ? "" : result + " = ") + "call " + arg1 + ", " + arg2;
            case CALL_INDIRECT -> (result == null ? "" : result + " = ") + "call *" + a + ", " + arg2;
            case LOAD_METHOD -> result + " = " + a + ".method[" + b + "]";
            case VTABLE -> "vtable class#" + arg1 + "[" + arg2 + "] = " + result;
            case RETURN -> arg1 == null ? "return" : "return " + a;
            case NEW -> result + " = new class#" + arg2 + " (" + arg1 + " bytes)";
            case NULL_CHECK -> "check_null " + a;
            case LOAD_FIELD -> result + " = " + a + "[" + b + "]";
            case STORE_FIELD -> b + "[" + result + "] = " + a;
            case NEW_ARRAY -> result + " = new array[" + a + "]";
            case LOAD_INDEX -> result + " = " + a + "[" + b + "]";
            case STORE_INDEX -> b + "[" + Operand.show(result) + "] = " + a;
            case READ_STR, READ_INT, READ_FLOAT, READ_CHAR -> result + " = " + op.name().toLowerCase();
            case STR_CONCAT -> result + " = concat " + a + ", " + b;
            case INT_TO_STR, CHAR_TO_STR, BOOL_TO_STR, FLOAT_TO_STR -> result + " = " + op.name().toLowerCase() + " " + a;
            case PRINT_STR, PRINT_INT, PRINT_CHAR, PRINT_BOOL, PRINT_FLOAT -> op.name().toLowerCase() + " " + a;
            case PRINT_NL -> "print_nl";
            default -> result + " = " + a + " " + op.symbol() + " " + b;
        };
    }

    // Operands that name a stack slot or a global; the frame layout is built from these.
    @JsonIgnore
    public List<String> variableOperands() {
        List<String> names = new ArrayList<>(3);
        switch (op) {
            case LABEL, GOTO, FUNC_BEGIN, FUNC_END, VTABLE -> {
            }
            case IF_FALSE, IF_TRUE, PARAM, RETURN, PRINT_STR, PRINT_INT, PRINT_CHAR, PRINT_BOOL, PRINT_FLOAT ->
                    names.add(arg1);
            case RECV, NEW, CALL -> names.add(result);
            case CALL_INDIRECT -> {
                names.add(arg1);
                names.add(result);
            }
            case STORE_FIELD -> {
                names.add(arg1);
                names.add(arg2);
            }
            default -> {
                names.add(arg1);
                names.add(arg2);
                names.add(result);
            }
        }
        names.removeIf(name -> !Operand.isVariable(name));
        return names;
    }

    @Override
    public String toString() {
        return format();
    }
}
