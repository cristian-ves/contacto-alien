package com.usac.contactoalien.contactoalienbackend.semantic.y.types;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructRegistry;

// Resolves a `type` node into a base YType (never carrying array dimensions: in this grammar
// brackets are written separately at each use site - variableDeclaration, arrayParameter -
// not inside `type` itself).
public final class YTypeResolver {

    private YTypeResolver() {}

    public static YType resolve(YParser.TypeContext ctx, YStructRegistry registry, YErrorReporter errors) {
        if (ctx.primitiveType() != null) {
            return resolvePrimitive(ctx.primitiveType());
        }
        String name = ctx.IDENTIFIER().getText();
        if (registry.exists(name)) {
            return YType.ofStruct(name);
        }
        errors.report("Structure '" + name + "' is not declared", ctx);
        return YType.UNKNOWN;
    }

    private static YType resolvePrimitive(YParser.PrimitiveTypeContext ctx) {
        if (ctx.ENTERO() != null) return YType.ENTERO;
        if (ctx.CADENA() != null) return YType.CADENA;
        if (ctx.FLOTANTE() != null) return YType.FLOTANTE;
        if (ctx.CARACTER() != null) return YType.CARACTER;
        return YType.BOOL;
    }
}