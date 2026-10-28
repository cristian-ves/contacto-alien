package com.usac.contactoalien.contactoalienbackend.codegen.quad;

import java.util.ArrayList;
import java.util.List;

// Accumulates the quadruples of one compilation and hands out fresh temporaries and labels.
public final class QuadBuilder {

    private final List<Quad> quads = new ArrayList<>();
    private final NameFactory names = new NameFactory();

    public NameFactory names() {
        return names;
    }

    public String temp() {
        return names.temp();
    }

    public String newLabel() {
        return names.label();
    }

    public void emit(QuadOp op, String arg1, String arg2, String result) {
        quads.add(new Quad(op, arg1, arg2, result));
    }

    // Emits an operation whose result is a fresh temporary and returns that temporary.
    public String emitTemp(QuadOp op, String arg1, String arg2) {
        String result = names.temp();
        emit(op, arg1, arg2, result);
        return result;
    }

    public void mark(String label) {
        emit(QuadOp.LABEL, null, null, label);
    }

    public void jump(String label) {
        emit(QuadOp.GOTO, null, null, label);
    }

    public void jumpIfFalse(String condition, String label) {
        emit(QuadOp.IF_FALSE, condition, null, label);
    }

    public void jumpIfTrue(String condition, String label) {
        emit(QuadOp.IF_TRUE, condition, null, label);
    }

    public void assign(String target, String value) {
        emit(QuadOp.ASSIGN, value, null, target);
    }

    public List<Quad> quads() {
        return List.copyOf(quads);
    }
}
