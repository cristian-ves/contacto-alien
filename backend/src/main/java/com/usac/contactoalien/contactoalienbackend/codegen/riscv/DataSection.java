package com.usac.contactoalien.contactoalienbackend.codegen.riscv;


import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
String literals and global variables of the program. A string is stored as a record: one
64-bit length (computed here, at compile time) followed by the bytes, so printing and
concatenation never have to search for a terminator.
*/
final class DataSection {

    private final Map<String, String> strings = new LinkedHashMap<>();
    private final Set<String> globals = new LinkedHashSet<>();
    // class id -> (slot -> label of the method that runs); one method table per class.
    private final Map<Integer, Map<Integer, String>> methodTables = new TreeMap<>();

    static final String CLASS_TABLE = "class_table";
    private static final String METHOD_TABLE_PREFIX = "vt_";

    void methodTableEntry(int classId, int slot, String label) {
        methodTables.computeIfAbsent(classId, id -> new TreeMap<>()).put(slot, label);
    }

    // Label of the record holding this literal; equal literals share one record.
    String stringLabel(String decodedText) {
        return strings.computeIfAbsent(decodedText, text -> "str_" + strings.size());
    }

    String globalLabel(String operand) {
        globals.add(operand);
        return "g_" + operand.substring(1);
    }

    void emit(AsmWriter out) {
        out.directive("    .section .rodata");
        for (Map.Entry<String, String> literal : strings.entrySet()) {
            byte[] bytes = literal.getKey().getBytes(StandardCharsets.UTF_8);
            out.directive("    .balign 8");
            out.label(literal.getValue());
            out.directive("    .quad " + bytes.length);
            if (bytes.length > 0) {
                out.directive("    .ascii \"" + escape(bytes) + "\"");
            }
        }
        emitMethodTables(out);
        out.blank();
        out.directive("    .bss");
        for (String global : globals) {
            out.directive("    .balign 8");
            out.label("g_" + global.substring(1));
            out.directive("    .space 8");
        }
    }

    // class_table has one pointer per class id (0 when the class has no methods to dispatch) and each
    // method table lists the addresses of the methods in slot order.
    private void emitMethodTables(AsmWriter out) {
        if (methodTables.isEmpty()) {
            return;
        }
        int lastId = methodTables.keySet().stream().max(Integer::compare).orElse(0);
        out.directive("    .balign 8");
        out.label(CLASS_TABLE);
        for (int id = 0; id <= lastId; id++) {
            out.directive("    .quad " + (methodTables.containsKey(id) ? METHOD_TABLE_PREFIX + id : "0"));
        }
        for (Map.Entry<Integer, Map<Integer, String>> table : methodTables.entrySet()) {
            out.directive("    .balign 8");
            out.label(METHOD_TABLE_PREFIX + table.getKey());
            for (String label : table.getValue().values()) {
                out.directive("    .quad " + label);
            }
        }
    }

    // Printable ASCII is written as is; everything else as a three-digit octal escape.
    private String escape(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte value : bytes) {
            int b = value & 0xff;
            if (b >= 0x20 && b < 0x7f && b != '"' && b != '\\') {
                out.append((char) b);
            } else {
                out.append('\\').append(String.format("%03o", b));
            }
        }
        return out.toString();
    }
}
