package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Quad;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
Stack frame of one function. s0 (the frame pointer) points at the saved s0:

    s0 + 16 + 8*i   parameter i (pushed by the caller, parameter 0 at the lowest address)
    s0 + 8          saved return address
    s0 + 0          saved caller s0
    s0 - 8*(k+1)    local variable or temporary number k

Every named operand of the function (variables and temporaries) gets one word; the frame size is
rounded to the stack alignment so sp stays aligned.
*/
final class FrameLayout {

    // Saved ra and s0 sit between the locals and the parameters.
    static final int SAVED_REGISTERS_BYTES = 2 * TargetConfig.WORD_SIZE;

    private final Map<String, Integer> offsets = new HashMap<>();
    private int localCount;

    FrameLayout(List<Quad> function) {
        int parameterCount = 0;
        for (Quad quad : function) {
            if (quad.op() == QuadOp.RECV) {
                offsets.put(quad.result(), SAVED_REGISTERS_BYTES + TargetConfig.WORD_SIZE * parameterCount++);
            }
        }
        for (Quad quad : function) {
            for (String name : quad.variableOperands()) {
                if (!Operand.isGlobal(name) && !offsets.containsKey(name)) {
                    localCount++;
                    offsets.put(name, -TargetConfig.WORD_SIZE * localCount);
                }
            }
        }
    }

    int offsetOf(String name) {
        return offsets.get(name);
    }

    int frameBytes() {
        return alignUp(localCount * TargetConfig.WORD_SIZE);
    }

    // Space an outgoing argument block of `count` words takes, kept aligned.
    static int argumentBytes(int count) {
        return alignUp(count * TargetConfig.WORD_SIZE);
    }

    private static int alignUp(int bytes) {
        int alignment = TargetConfig.STACK_ALIGNMENT;
        return (bytes + alignment - 1) / alignment * alignment;
    }
}
