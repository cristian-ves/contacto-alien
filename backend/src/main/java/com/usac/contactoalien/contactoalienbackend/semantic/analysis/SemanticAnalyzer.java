package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.Scope;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.VariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.types.TypeResolver;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

// Orchestrates the semantic analysis of one Zetariano class in two explicit phases, since a
// class may now reference other classes from the same compilation batch:
//   1) collectSymbols: builds this class's own ClassSymbolTable. The caller must register the
//      returned table in the CompilationUnitRegistry before collecting or analyzing any class
//      that references this one.
//   2) analyzeBodies: type-checks every constructor/method body. Requires every class this
//      class's code refers to (including itself) to already be registered.
// Both phases report into the same ErrorReporter, retrievable via getErrors().
public final class SemanticAnalyzer {

    private final ErrorReporter errors = new ErrorReporter();

    public ClassSymbolTable collectSymbols(ZetarianoParser.ClassDeclarationContext classCtx, CompilationUnitRegistry registry) {
        return new com.usac.contactoalien.contactoalienbackend.semantic.analysis.ClassSymbolCollector(errors, registry).collect(classCtx);
    }

    public void analyzeBodies(ZetarianoParser.ClassDeclarationContext classCtx, ClassSymbolTable classSymbolTable, CompilationUnitRegistry registry) {
        for (ZetarianoParser.ClassMemberContext member : classCtx.classMember()) {
            if (member.constructorDeclaration() != null) {
                analyzeConstructor(member.constructorDeclaration(), classSymbolTable, registry);
            } else if (member.methodDeclaration() != null) {
                analyzeMethod(member.methodDeclaration(), classSymbolTable, registry);
            }
        }
    }

    public ErrorReporter getErrors() {
        return errors;
    }

    private void analyzeConstructor(ZetarianoParser.ConstructorDeclarationContext ctx, ClassSymbolTable classSymbolTable, CompilationUnitRegistry registry) {
        Scope parameterScope = new Scope(null);
        declareParameters(ctx.parameterList(), parameterScope, classSymbolTable, registry);
        new StatementAnalyzer(classSymbolTable, registry, errors).analyzeBody(ctx.block(), parameterScope, ZType.VOID);
    }

    private void analyzeMethod(ZetarianoParser.MethodDeclarationContext ctx, ClassSymbolTable classSymbolTable, CompilationUnitRegistry registry) {
        ZType returnType = ctx.VOID() != null ? ZType.VOID : TypeResolver.resolve(ctx.type(), classSymbolTable, registry, errors);
        Scope parameterScope = new Scope(null);
        declareParameters(ctx.parameterList(), parameterScope, classSymbolTable, registry);
        new StatementAnalyzer(classSymbolTable, registry, errors).analyzeBody(ctx.block(), parameterScope, returnType);
    }

    private void declareParameters(ZetarianoParser.ParameterListContext ctx, Scope scope, ClassSymbolTable classSymbolTable, CompilationUnitRegistry registry) {
        if (ctx == null) return;
        for (ZetarianoParser.ParameterContext p : ctx.parameter()) {
            ZType type = TypeResolver.resolve(p.type(), classSymbolTable, registry, errors);
            String name = p.IDENTIFIER().getText();
            if (!scope.declare(new VariableSymbol(name, type))) {
                errors.report("Parameter '" + name + "' is already declared in this parameter list", p);
            }
        }
    }
}