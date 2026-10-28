package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

// Collects assembly text: instructions are indented, labels and directives start at the margin.
final class AsmWriter {

    private final StringBuilder text = new StringBuilder();

    void instruction(String line) {
        text.append("    ").append(line).append('\n');
    }

    void label(String name) {
        text.append(name).append(":\n");
    }

    void directive(String line) {
        text.append(line).append('\n');
    }

    void comment(String line) {
        text.append("# ").append(line).append('\n');
    }

    void blank() {
        text.append('\n');
    }

    void raw(String block) {
        text.append(block);
    }

    String text() {
        return text.toString();
    }
}
