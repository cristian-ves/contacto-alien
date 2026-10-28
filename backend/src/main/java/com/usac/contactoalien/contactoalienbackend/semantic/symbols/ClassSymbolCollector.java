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

    public ClassSymbolTable collect(ZetarianoParser.ClassDeclarationContext ctx, String packageName) {
        String className = ctx.IDENTIFIER(0).getText();
        classSymbolTable = new ClassSymbolTable(className);
        classSymbolTable.setPackageName(packageName);
        if (ctx.EXTENDS() != null) {
            classSymbolTable.setSuperName(ctx.IDENTIFIER(1).getText());
        }
        for (ZetarianoParser.ClassMemberContext member : ctx.classMember()) {
            visit(member);
        }
        return classSymbolTable;
    }

    @Override
    public Void visitFieldDeclaration(ZetarianoParser.FieldDeclarationContext ctx) {
        ZType type = TypeResolver.resolve(ctx.type(), classSymbolTable, registry, errors);
        String name = ctx.IDENTIFIER().getText();
        if (!classSymbolTable.declareField(new VariableSymbol(name, type,
                visibilityOf(ctx.visibilityModifier()), classSymbolTable.getClassName()))) {
            errors.report("Field '" + name + "' is already declared in class '" + classSymbolTable.getClassName() + "'", ctx);
        }
        return null;
    }

    @Override
    public Void visitConstructorDeclaration(ZetarianoParser.ConstructorDeclarationContext ctx) {
        List<ZType> parameters = collectParameterTypes(ctx.parameterList());
        for (ConstructorSymbol existing : classSymbolTable.getConstructors()) {
            if (existing.getParameterTypes().equals(parameters)) {
                errors.report("Constructor '" + classSymbolTable.getClassName() + "' with the same parameter types is already declared", ctx);
            }
        }
        classSymbolTable.addConstructor(new ConstructorSymbol(parameters, visibilityOf(ctx.visibilityModifier())));
        return null;
    }

    @Override
    public Void visitMethodDeclaration(ZetarianoParser.MethodDeclarationContext ctx) {
        ZType returnType = ctx.VOID() != null ? ZType.VOID : TypeResolver.resolve(ctx.type(), classSymbolTable, registry, errors);
        String name = ctx.IDENTIFIER().getText();
        List<ZType> parameters = collectParameterTypes(ctx.parameterList());
        for (MethodSymbol existing : classSymbolTable.resolveMethods(name)) {
            if (existing.getParameterTypes().equals(parameters)) {
                errors.report("Method '" + name + "' with the same parameter types is already declared in class '"
                        + classSymbolTable.getClassName() + "'", ctx);
            }
        }
        classSymbolTable.addMethod(new MethodSymbol(name, returnType, parameters,
                visibilityOf(ctx.visibilityModifier()), classSymbolTable.getClassName(), ctx.OVERRIDE() != null));
        return null;
    }

    // No modifier means default (package) access.
    private static Visibility visibilityOf(ZetarianoParser.VisibilityModifierContext modifier) {
        if (modifier == null) return Visibility.DEFAULT;
        if (modifier.PUBLIC() != null) return Visibility.PUBLIC;
        if (modifier.PROTECTED() != null) return Visibility.PROTECTED;
        return Visibility.PRIVATE;
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