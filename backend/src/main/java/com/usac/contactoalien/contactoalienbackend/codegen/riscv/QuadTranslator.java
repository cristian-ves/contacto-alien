package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Quad;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;

/**
Translates one quadruple at a time into RISC-V instructions. Every operation follows the
same pattern: load the operands into temporaries (t0, t1), compute, store the result in its
slot. Runtime routines receive arguments in a0/a1 and return in a0, as in the standard ABI.
Calls and returns are handled by CallTranslator.
*/
final class QuadTranslator {

    static final String LABEL_PREFIX = ".L_";

    private final AsmWriter out;
    private final OperandAccess operands;
    private final CallTranslator calls;
    private int skipLabels;

    QuadTranslator(AsmWriter out, OperandAccess operands, CallTranslator calls) {
        this.out = out;
        this.operands = operands;
        this.calls = calls;
    }

    void translate(Quad quad) {
        out.comment(quad.format().replace("\n", "\\n"));
        switch (quad.op()) {
            case LABEL -> out.label(LABEL_PREFIX + quad.result());
            case GOTO -> out.instruction("j " + LABEL_PREFIX + quad.result());
            case IF_FALSE -> branch(quad, "bnez");
            case IF_TRUE -> branch(quad, "beqz");
            case ASSIGN -> unary(quad, null);
            case NEG -> unary(quad, "neg t0, t0");
            case NOT -> unary(quad, "seqz t0, t0");
            case ADD -> binary(quad, "add t0, t0, t1");
            case SUB -> binary(quad, "sub t0, t0, t1");
            case MUL -> binary(quad, "mul t0, t0, t1");
            case DIV -> divide(quad, "div t0, t0, t1");
            case MOD -> divide(quad, "rem t0, t0, t1");
            case AND -> binary(quad, "and t0, t0, t1");
            case OR -> binary(quad, "or t0, t0, t1");
            case LT -> binary(quad, "slt t0, t0, t1");
            case GT -> binary(quad, "slt t0, t1, t0");
            case LE -> binary(quad, "slt t0, t1, t0", "xori t0, t0, 1");
            case GE -> binary(quad, "slt t0, t0, t1", "xori t0, t0, 1");
            case EQ -> binary(quad, "sub t0, t0, t1", "seqz t0, t0");
            case NE -> binary(quad, "sub t0, t0, t1", "snez t0, t0");
            case FADD -> floatBinary(quad, "fadd.d ft0, ft0, ft1", true);
            case FSUB -> floatBinary(quad, "fsub.d ft0, ft0, ft1", true);
            case FMUL -> floatBinary(quad, "fmul.d ft0, ft0, ft1", true);
            case FDIV -> floatBinary(quad, "fdiv.d ft0, ft0, ft1", true);
            case FMOD -> floatRemainder(quad);
            case FLT -> floatBinary(quad, "flt.d t0, ft0, ft1", false);
            case FGT -> floatBinary(quad, "flt.d t0, ft1, ft0", false);
            case FLE -> floatBinary(quad, "fle.d t0, ft0, ft1", false);
            case FGE -> floatBinary(quad, "fle.d t0, ft1, ft0", false);
            case FEQ -> floatBinary(quad, "feq.d t0, ft0, ft1", false);
            case FNE -> floatBinary(quad, "feq.d t0, ft0, ft1", false, "xori t0, t0, 1");
            case FNEG -> {
                operands.load("t0", quad.arg1());
                out.instruction("fmv.d.x ft0, t0");
                out.instruction("fneg.d ft0, ft0");
                out.instruction("fmv.x.d t0, ft0");
                operands.store("t0", quad.result());
            }
            case I2F -> {
                operands.load("t0", quad.arg1());
                out.instruction("fcvt.d.l ft0, t0");
                out.instruction("fmv.x.d t0, ft0");
                operands.store("t0", quad.result());
            }
            case F2I -> {
                operands.load("t0", quad.arg1());
                out.instruction("fmv.d.x ft0, t0");
                out.instruction("fcvt.l.d t0, ft0, rtz");
                operands.store("t0", quad.result());
            }
            case RECV, FUNC_BEGIN, FUNC_END -> {
            }
            case PARAM -> calls.param(quad.arg1());
            case CALL -> calls.call(quad);
            case CALL_INDIRECT -> calls.callIndirect(quad);
            case VTABLE -> { }
            case LOAD_METHOD -> loadMethod(quad);
            case RETURN -> calls.returnValue(quad.arg1());
            case NEW -> allocate(quad);
            case NULL_CHECK -> {
                operands.load("t0", quad.arg1());
                out.instruction("beqz t0, null_fail");
            }
            case LOAD_FIELD -> {
                operands.load("t0", quad.arg1());
                out.instruction("beqz t0, null_fail");
                out.instruction("ld t0, " + quad.arg2() + "(t0)");
                operands.store("t0", quad.result());
            }
            case STORE_FIELD -> {
                operands.load("t0", quad.arg1());
                operands.load("t1", quad.arg2());
                out.instruction("beqz t1, null_fail");
                out.instruction("sd t0, " + quad.result() + "(t1)");
            }
            case NEW_ARRAY -> convert(quad, "array_new");
            case LOAD_INDEX -> {
                operands.load("a0", quad.arg1());
                operands.load("a1", quad.arg2());
                out.instruction("call array_addr");
                out.instruction("ld t0, 0(a0)");
                operands.store("t0", quad.result());
            }
            case STORE_INDEX -> {
                operands.load("a0", quad.arg2());
                operands.load("a1", quad.result());
                out.instruction("call array_addr");
                operands.load("t0", quad.arg1());
                out.instruction("sd t0, 0(a0)");
            }
            case STR_EQ -> {
                operands.load("a0", quad.arg1());
                operands.load("a1", quad.arg2());
                out.instruction("call str_eq");
                operands.store("a0", quad.result());
            }
            case STR_CONCAT -> {
                operands.load("a0", quad.arg1());
                operands.load("a1", quad.arg2());
                out.instruction("call str_concat");
                operands.store("a0", quad.result());
            }
            case INT_TO_STR -> convert(quad, "int_to_str");
            case CHAR_TO_STR -> convert(quad, "char_to_str");
            case BOOL_TO_STR -> convert(quad, "bool_to_str");
            case FLOAT_TO_STR -> convert(quad, "float_to_str");
            case READ_STR -> readInto(quad, "read_line");
            case READ_INT -> readInto(quad, "read_int");
            case READ_FLOAT -> readInto(quad, "read_float");
            case READ_CHAR -> readInto(quad, "read_char");
            case PRINT_STR -> print(quad, "print_str");
            case PRINT_INT -> print(quad, "print_int");
            case PRINT_CHAR -> print(quad, "print_char");
            case PRINT_BOOL -> print(quad, "print_bool");
            case PRINT_FLOAT -> print(quad, "print_float");
            case PRINT_NL -> out.instruction("call print_nl");
        }
    }

    // Conditional jumps are written as an inverted short branch over a `j`, because a plain
    // conditional branch reaches only +-4 KiB while `j` reaches +-1 MiB.
    private void branch(Quad quad, String skipInstruction) {
        String skip = ".L_skip" + skipLabels++;
        operands.load("t0", quad.arg1());
        out.instruction(skipInstruction + " t0, " + skip);
        out.instruction("j " + LABEL_PREFIX + quad.result());
        out.label(skip);
    }

    private void unary(Quad quad, String compute) {
        operands.load("t0", quad.arg1());
        if (compute != null) {
            out.instruction(compute);
        }
        operands.store("t0", quad.result());
    }

    private void binary(Quad quad, String... compute) {
        operands.load("t0", quad.arg1());
        operands.load("t1", quad.arg2());
        for (String instruction : compute) {
            out.instruction(instruction);
        }
        operands.store("t0", quad.result());
    }

    // A decimal travels as the bits of a double in an ordinary word; it is moved into floating-point
    // registers only for the operation. Comparisons leave an integer 0/1 in t0 instead.
    private void floatBinary(Quad quad, String compute, boolean floatResult, String... after) {
        operands.load("t0", quad.arg1());
        operands.load("t1", quad.arg2());
        out.instruction("fmv.d.x ft0, t0");
        out.instruction("fmv.d.x ft1, t1");
        out.instruction(compute);
        if (floatResult) {
            out.instruction("fmv.x.d t0, ft0");
        }
        for (String instruction : after) {
            out.instruction(instruction);
        }
        operands.store("t0", quad.result());
    }

    // The remainder of two decimals is computed by a runtime routine (there is no instruction for it).
    private void floatRemainder(Quad quad) {
        operands.load("a0", quad.arg1());
        operands.load("a1", quad.arg2());
        out.instruction("call float_rem");
        operands.store("a0", quad.result());
    }

    private void readInto(Quad quad, String routine) {
        out.instruction("call " + routine);
        operands.store("a0", quad.result());
    }

    // Integer division traps nothing in hardware (x/0 is -1), so a zero divisor is caught before dividing.
    private void divide(Quad quad, String compute) {
        String ok = ".L_skip" + skipLabels++;
        operands.load("t0", quad.arg1());
        operands.load("t1", quad.arg2());
        out.instruction("bnez t1, " + ok);
        out.instruction("j div_zero");
        out.label(ok);
        out.instruction(compute);
        operands.store("t0", quad.result());
    }

    // The class id sits in word 0 of the object; class_table maps it to the class's method table.
    private void loadMethod(Quad quad) {
        operands.load("t0", quad.arg1());
        out.instruction("beqz t0, null_fail");
        out.instruction("ld t0, 0(t0)");
        out.instruction("slli t0, t0, 3");
        out.instruction("la t1, " + DataSection.CLASS_TABLE);
        out.instruction("add t1, t1, t0");
        out.instruction("ld t1, 0(t1)");
        out.instruction("ld t0, " + Integer.parseInt(quad.arg2()) * TargetConfig.WORD_SIZE + "(t1)");
        operands.store("t0", quad.result());
    }

    private void allocate(Quad quad) {
        out.instruction("li a0, " + quad.arg1());
        out.instruction("call alloc");
        out.instruction("li t0, " + quad.arg2());
        out.instruction("sd t0, 0(a0)");
        operands.store("a0", quad.result());
    }

    private void convert(Quad quad, String routine) {
        operands.load("a0", quad.arg1());
        out.instruction("call " + routine);
        operands.store("a0", quad.result());
    }

    private void print(Quad quad, String routine) {
        operands.load("a0", quad.arg1());
        out.instruction("call " + routine);
    }
}
