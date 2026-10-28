package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.*;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeResolver;

// Orchestrates the semantic analysis of a Y? program: collects every structure, then every
// function signature, then analyzes each function body. The struct/function registries built
// during analyze() are kept as fields (instead of local variables) so callers outside this
// class, such as the Pig Latin import loader, can inspect them afterward.
public final class YSemanticAnalyzer {

    private final YErrorReporter errors = new YErrorReporter();
    private YStructRegistry structRegistry;
    private YFunctionRegistry functionRegistry;

    public YErrorReporter analyze(YParser.ProgramContext programCtx) {
        structRegistry = new YStructRegistry();
        new YStructCollector(structRegistry, errors).collect(programCtx);

        functionRegistry = new YFunctionRegistry();
        new YFunctionCollector(structRegistry, functionRegistry, errors).collect(programCtx.functionsSection());

        for (YParser.FunctionDeclarationContext fn : programCtx.functionsSection().functionDeclaration()) {
            analyzeFunction(fn, structRegistry, functionRegistry);
        }
        return errors;
    }

    public YErrorReporter getErrors() { return errors; }
    public YStructRegistry getStructRegistry() { return structRegistry; }
    public YFunctionRegistry getFunctionRegistry() { return functionRegistry; }

    private void analyzeFunction(YParser.FunctionDeclarationContext ctx, YStructRegistry structRegistry, YFunctionRegistry functionRegistry) {
        YType returnType = ctx.type() != null ? YTypeResolver.resolve(ctx.type(), structRegistry, errors) : YType.VOID;
        YScope parameterScope = new YScope(null);
        if (ctx.parameterList() != null) {
            for (YParser.ParameterContext p : ctx.parameterList().parameter()) {
                YType type = YParameterResolver.resolveType(p, structRegistry, errors);
                String name = YParameterResolver.resolveName(p);
                if (!parameterScope.declare(new YVariableSymbol(name, type))) {
                    errors.report("Parameter '" + name + "' is already declared in this parameter list", p);
                }
            }
        }
        new YStatementAnalyzer(structRegistry, functionRegistry, errors).analyzeBody(ctx.statement(), parameterScope, returnType);
        new YReturnFlowChecker(errors).check(ctx, returnType.getKind() != YType.Kind.VOID && !returnType.isUnknown());
    }
}