package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YFunctionRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YFunctionSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeResolver;

import java.util.ArrayList;
import java.util.List;

// Collects every function's signature (name, parameter types, return type) before any function
// body is analyzed, so functions may call each other regardless of declaration order.
public final class YFunctionCollector {

    private final YStructRegistry structRegistry;
    private final YFunctionRegistry functionRegistry;
    private final YErrorReporter errors;

    public YFunctionCollector(YStructRegistry structRegistry, YFunctionRegistry functionRegistry, YErrorReporter errors) {
        this.structRegistry = structRegistry;
        this.functionRegistry = functionRegistry;
        this.errors = errors;
    }

    public void collect(YParser.FunctionsSectionContext ctx) {
        for (YParser.FunctionDeclarationContext fn : ctx.functionDeclaration()) {
            String name = fn.IDENTIFIER().getText();
            YType returnType = fn.type() != null ? YTypeResolver.resolve(fn.type(), structRegistry, errors) : YType.VOID;
            List<YType> paramTypes = new ArrayList<>();
            if (fn.parameterList() != null) {
                for (YParser.ParameterContext p : fn.parameterList().parameter()) {
                    paramTypes.add(YParameterResolver.resolveType(p, structRegistry, errors));
                }
            }
            if (!functionRegistry.declare(new YFunctionSymbol(name, paramTypes, returnType))) {
                errors.report("Function '" + name + "' is already declared", fn);
            }
        }
    }
}