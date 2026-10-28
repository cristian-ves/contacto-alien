package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;

/**
Moves quadruple operands between memory and registers. Literals are loaded as immediates or
as addresses of string records, globals through their label, everything else from its frame
slot. t6 is reserved as the address scratch register so values never collide with it.
*/
final class OperandAccess {

    private static final int IMMEDIATE_MIN = -2048;
    private static final int IMMEDIATE_MAX = 2047;

    private final AsmWriter out;
    private final DataSection data;
    private FrameLayout frame;

    OperandAccess(AsmWriter out, DataSection data) {
        this.out = out;
        this.data = data;
    }

    void enter(FrameLayout frame) {
        this.frame = frame;
    }

    void load(String register, String operand) {
        if (Operand.isIntLiteral(operand)) {
            out.instruction("li " + register + ", " + operand);
        } else if (Operand.isFloatLiteral(operand)) {
            out.instruction("li " + register + ", " + Operand.floatBits(operand));
        } else if (Operand.isStringLiteral(operand)) {
            out.instruction("la " + register + ", " + data.stringLabel(Operand.stringValue(operand)));
        } else if (Operand.isGlobal(operand)) {
            out.instruction("la " + register + ", " + data.globalLabel(operand));
            out.instruction("ld " + register + ", 0(" + register + ")");
        } else {
            memory("ld", register, frame.offsetOf(operand));
        }
    }

    void store(String register, String result) {
        if (Operand.isGlobal(result)) {
            out.instruction("la t6, " + data.globalLabel(result));
            out.instruction("sd " + register + ", 0(t6)");
        } else {
            memory("sd", register, frame.offsetOf(result));
        }
    }

    // A frame slot beyond the 12-bit offset range is reached through an explicitly computed address.
    private void memory(String instruction, String register, int offset) {
        if (offset >= IMMEDIATE_MIN && offset <= IMMEDIATE_MAX) {
            out.instruction(instruction + " " + register + ", " + offset + "(s0)");
        } else {
            out.instruction("li t6, " + offset);
            out.instruction("add t6, s0, t6");
            out.instruction(instruction + " " + register + ", 0(t6)");
        }
    }
}
