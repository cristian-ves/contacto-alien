package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.NameFactory;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
Nested block scopes for one function plus the program-level globals. Each variable keeps
the operand name the quadruples use for it (a global is prefixed with '@').
*/
public final class LocalScope {

    public record Variable(String operand, GenType type) {
    }

    private final NameFactory names;
    private final Map<String, Variable> globals = new HashMap<>();
    private final Deque<Map<String, Variable>> blocks = new ArrayDeque<>();

    public LocalScope(NameFactory names) {
        this.names = names;
    }

    public void push() {
        blocks.push(new HashMap<>());
    }

    public void pop() {
        blocks.pop();
    }

    public Variable declareLocal(String name, GenType type) {
        Variable variable = new Variable(names.variable(name), type);
        blocks.peek().put(name, variable);
        return variable;
    }

    public Variable declareGlobal(String name, GenType type) {
        Variable variable = new Variable(Operand.global(name), type);
        globals.put(name, variable);
        return variable;
    }

    // Innermost block first, then the globals; null when the name is not declared.
    public Variable lookup(String name) {
        for (Map<String, Variable> block : blocks) {
            Variable found = block.get(name);
            if (found != null) {
                return found;
            }
        }
        return globals.get(name);
    }
}
