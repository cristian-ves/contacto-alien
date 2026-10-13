package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParserBaseVisitor;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.*;
import com.usac.contactoalien.contactoalienbackend.semantic.types.TypeResolver;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.ArrayList;
import java.util.List;

// First pass: walks the class declaration and builds the class-level symbol table (fields,
// method signatures, constructor signatures) without inspecting method bodies. Cross-class
// field/parameter/return types are resolved through the CompilationUnitRegistry, so any other
// class already registered in the same batch counts as a known type.
public final class ClassSymbolCollector extends ZetarianoParserBaseVisitor<Void> {

    private final ErrorReporter errors;
    private final CompilationUnitRegistry registry;
    private ClassSymbolTable classSymbolTable;

    public ClassSymbolCollector(ErrorReporter errors, CompilationUnitRegistry registry) {
        this.errors = errors;
        this.registry = registry;
    }

    public ClassSymbolTable collect(ZetarianoParser.ClassDeclarationContext ctx) {
        String className = ctx.IDENTIFIER().getText();
        classSymbolTable = new ClassSymbolTable(className);
        for (ZetarianoParser.ClassMemberContext member : ctx.classMember()) {
            visit(member);
        }
        return classSymbolTable;
    }

    @Override
    public Void visitFieldDeclaration(ZetarianoParser.FieldDeclarationContext ctx) {
        ZType type = TypeResolver.resolve(ctx.type(), classSymbolTable, registry, errors);
        String name = ctx.IDENTIFIER().getText();
        if (!classSymbolTable.declareField(new VariableSymbol(name, type))) {
            errors.report("Field '" + name + "' is already declared in class '" + classSymbolTable.getClassName() + "'", ctx);
        }
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(ZetarianoParser.ConstructorDeclarationContext ctx) {
        classSymbolTable.addConstructor(new ConstructorSymbol(collectParameterTypes(ctx.parameterList())));
        return null;
    }

    @Override
    public Void visitMethodDeclaration(ZetarianoParser.MethodDeclarationContext ctx) {
        ZType returnType = ctx.VOID() != null ? ZType.VOID : TypeResolver.resolve(ctx.type(), classSymbolTable, registry, errors);
        String name = ctx.IDENTIFIER().getText();
        classSymbolTable.addMethod(new MethodSymbol(name, returnType, collectParameterTypes(ctx.parameterList())));
        return null;
    }

    private List<ZType> collectParameterTypes(ZetarianoParser.ParameterListContext ctx) {
        List<ZType> types = new ArrayList<>();
        if (ctx == null) return types;
        for (ZetarianoParser.ParameterContext p : ctx.parameter()) {
            types.add(TypeResolver.resolve(p.type(), classSymbolTable, registry, errors));
        }
        return types;
    }
}