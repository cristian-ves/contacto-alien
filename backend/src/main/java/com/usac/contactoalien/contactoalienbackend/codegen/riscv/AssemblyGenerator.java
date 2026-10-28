package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.Quad;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;

import java.util.ArrayList;
import java.util.List;

/**
Turns the quadruples of a whole program into one RV64 GNU assembler file: the entry point,
the runtime routines, one routine per FUNC_BEGIN..FUNC_END block, and the data (string
literals and globals). Nothing is generated from templates outside this package.
*/
public final class AssemblyGenerator {

    private static final String ENTRY_SYMBOL = "_start";

    public String generate(List<Quad> quads, String entryLabel) {
        AsmWriter out = new AsmWriter();
        DataSection data = new DataSection();
        OperandAccess operands = new OperandAccess(out, data);
        CallTranslator calls = new CallTranslator(out, operands);
        QuadTranslator translator = new QuadTranslator(out, operands, calls);

        for (Quad quad : quads) {
            if (quad.op() == QuadOp.VTABLE) {
                data.methodTableEntry(Integer.parseInt(quad.arg1()), Integer.parseInt(quad.arg2()), quad.result());
            }
        }

        out.comment("Generated RISC-V (RV64) program");
        out.directive("    .text");
        out.directive("    .globl " + ENTRY_SYMBOL);
        out.label(ENTRY_SYMBOL);
        out.comment("Run the program, then exit with status 0");
        out.instruction("call stack_init");
        out.instruction("call " + entryLabel);
        out.instruction("li a0, 0");
        out.instruction("j exit");
        out.blank();

        for (List<Quad> function : split(quads)) {
            function(function, out, operands, calls, translator);
        }
        out.raw(RuntimeLibrary.text());
        out.blank();
        data.emit(out);
        return out.text();
    }

    private void function(List<Quad> body, AsmWriter out, OperandAccess operands, CallTranslator calls,
                          QuadTranslator translator) {
        String label = body.get(0).result();
        FrameLayout frame = new FrameLayout(body);
        operands.enter(frame);
        calls.enter(label);
        out.comment("function " + label);
        calls.prologue(label, frame.frameBytes());
        for (Quad quad : body) {
            translator.translate(quad);
        }
        calls.epilogue(label);
        out.blank();
    }

    // Groups the flat quadruple list into its functions.
    private List<List<Quad>> split(List<Quad> quads) {
        List<List<Quad>> functions = new ArrayList<>();
        List<Quad> current = null;
        for (Quad quad : quads) {
            if (quad.op() == QuadOp.FUNC_BEGIN) {
                current = new ArrayList<>();
                functions.add(current);
            }
            if (current != null) {
                current.add(quad);
            }
        }
        return functions;
    }
}
