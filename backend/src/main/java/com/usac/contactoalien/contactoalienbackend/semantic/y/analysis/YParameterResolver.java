package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeResolver;

// Resolves the type and name of a parameter regardless of which of the three syntactic forms
// (scalar, array, structure) was used to declare it.
public final class YParameterResolver {

    private YParameterResolver() {}

    public static YType resolveType(YParser.ParameterContext ctx, YStructRegistry registry, YErrorReporter errors) {
        if (ctx instanceof YParser.ScalarParameterContext scalar) {
            return YTypeResolver.resolve(scalar.type(), registry, errors);
        }
        if (ctx instanceof YParser.ArrayParameterContext array) {
            return YTypeResolver.resolve(array.type(), registry, errors).asArray(1);
        }
        YParser.StructParameterContext struct = (YParser.StructParameterContext) ctx;
        String structName = struct.IDENTIFIER(0).getText();
        if (!registry.exists(structName)) {
            errors.report("Structure '" + structName + "' is not declared", struct);
            return YType.UNKNOWN;
        }
        return YType.ofStruct(structName);
    }

    public static String resolveName(YParser.ParameterContext ctx) {
        if (ctx instanceof YParser.ScalarParameterContext scalar) return scalar.IDENTIFIER().getText();
        if (ctx instanceof YParser.ArrayParameterContext array) return array.IDENTIFIER().getText();
        YParser.StructParameterContext struct = (YParser.StructParameterContext) ctx;
        return struct.IDENTIFIER(1).getText();
    }
}