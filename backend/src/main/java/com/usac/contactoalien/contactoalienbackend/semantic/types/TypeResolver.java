package com.usac.contactoalien.contactoalienbackend.semantic.types;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;

// Resolves a `type` parse node into a ZType. A non-primitive name is a valid class type when it
// is either the class currently being declared, or any other class registered in the
// CompilationUnitRegistry for this compilation batch; otherwise it is an unknown type.
public final class TypeResolver {

    private TypeResolver() {}

    public static ZType resolve(ZetarianoParser.TypeContext ctx, ClassSymbolTable classTable, CompilationUnitRegistry registry, ErrorReporter errors) {
        ZType base;
        if (ctx.INT() != null) base = ZType.INT;
        else if (ctx.DOUBLE() != null) base = ZType.DOUBLE;
        else if (ctx.CHAR() != null) base = ZType.CHAR;
        else if (ctx.BOOLEAN() != null) base = ZType.BOOLEAN;
        else if (ctx.STRING() != null) base = ZType.STRING;
        else {
            String name = ctx.IDENTIFIER().getText();
            if (name.equals(classTable.getClassName())) {
                base = classTable.getOwnType();
            } else if (registry != null && registry.contains(name)) {
                base = ZType.ofClass(name);
            } else {
                errors.report("Unknown type '" + name + "'; no class named '" + name + "' is declared or registered", ctx);
                base = ZType.UNKNOWN;
            }
        }
        int dims = ctx.LBRACKET().size();
        return dims > 0 ? base.asArray(dims) : base;
    }
}