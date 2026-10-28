package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Quad;

import java.util.ArrayList;
import java.util.List;

/**
Function calling convention. The caller reserves a block on the stack, stores the arguments in
it (argument 0 at the lowest address), calls, and releases the block. The callee reads its
parameters through s0 (see FrameLayout) and returns its result in a0.
*/
final class CallTranslator {

    private static final String RETURN_PREFIX = ".R_";

    private final AsmWriter out;
    private final OperandAccess operands;
    private final List<String> pending = new ArrayList<>();
    private String currentFunction;

    CallTranslator(AsmWriter out, OperandAccess operands) {
        this.out = out;
        this.operands = operands;
    }

    void enter(String functionLabel) {
        currentFunction = functionLabel;
        pending.clear();
    }

    void param(String operand) {
        pending.add(operand);
    }

    void call(Quad quad) {
        call(quad, false);
    }

    // Same convention for a call through an address held in an operand (dynamic dispatch).
    void callIndirect(Quad quad) {
        call(quad, true);
    }

    private void call(Quad quad, boolean indirect) {
        int count = pending.size();
        int block = FrameLayout.argumentBytes(count);
        if (count > 0) {
            out.instruction("addi sp, sp, -" + block);
            for (int i = 0; i < count; i++) {
                operands.load("t0", pending.get(i));
                out.instruction("sd t0, " + i * TargetConfig.WORD_SIZE + "(sp)");
            }
        }
        if (indirect) {
            operands.load("t1", quad.arg1());
            out.instruction("jalr ra, 0(t1)");
        } else {
            out.instruction("call " + quad.arg1());
        }
        if (count > 0) {
            out.instruction("addi sp, sp, " + block);
        }
        pending.clear();
        if (quad.result() != null) {
            operands.store("a0", quad.result());
        }
    }

    void returnValue(String operand) {
        if (operand != null) {
            operands.load("a0", operand);
        }
        out.instruction("j " + RETURN_PREFIX + currentFunction);
    }

    // Saves ra and the caller's s0, points s0 at them and reserves room for the locals.
    void prologue(String label, int frameBytes) {
        out.label(label);
        out.instruction("addi sp, sp, -" + FrameLayout.SAVED_REGISTERS_BYTES);
        out.instruction("sd ra, " + TargetConfig.WORD_SIZE + "(sp)");
        out.instruction("sd s0, 0(sp)");
        out.instruction("mv s0, sp");
        if (frameBytes > 0) {
            out.instruction("li t0, " + frameBytes);
            out.instruction("sub sp, sp, t0");
        }
        // A runaway recursion stops here with a message instead of faulting on unmapped memory.
        out.instruction("la t1, stack_limit");
        out.instruction("ld t1, 0(t1)");
        out.instruction("bltu sp, t1, stack_overflow");
    }

    void epilogue(String label) {
        out.label(RETURN_PREFIX + label);
        out.instruction("mv sp, s0");
        out.instruction("ld ra, " + TargetConfig.WORD_SIZE + "(sp)");
        out.instruction("ld s0, 0(sp)");
        out.instruction("addi sp, sp, " + FrameLayout.SAVED_REGISTERS_BYTES);
        out.instruction("ret");
    }
}
