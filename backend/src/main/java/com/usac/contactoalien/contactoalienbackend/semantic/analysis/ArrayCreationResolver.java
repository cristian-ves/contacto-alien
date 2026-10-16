package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.ArrayCreationContext;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.ExpressionContext;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.List;

/**
Resolves the ZType of an array creation expression (e.g. 'new int[n]', 'new char[tamanio]',
'new boolean[f][c]'), verifying that every dimension expression evaluates to int and
confirming that any class name used as the element type exists.
*/
public final class ArrayCreationResolver {

    private final ExpressionTypeResolver expressionResolver;
    private final ClassSymbolTable classSymbolTable;
    private final CompilationUnitRegistry registry;
    private final ErrorReporter errors;

    public ArrayCreationResolver(ExpressionTypeResolver expressionResolver,
                                ClassSymbolTable classSymbolTable,
                                CompilationUnitRegistry registry,
                                ErrorReporter errors) {
        this.expressionResolver = expressionResolver;
        this.classSymbolTable = classSymbolTable;
        this.registry = registry;
        this.errors = errors;
    }

    public ZType resolve(ArrayCreationContext ctx) {
        List<ExpressionContext> dimExpressions = ctx.expression();
        for (ExpressionContext dimExpr : dimExpressions) {
            ZType dimType = expressionResolver.resolve(dimExpr);
            if (!dimType.isUnknown() && dimType.getKind() != ZType.Kind.INT) {
                errors.report("Array dimension must be of type int, found '" + dimType + "'", dimExpr);
            }
        }

        ZType baseType = resolveBaseType(ctx);
        int dimensions = dimExpressions.size();
        return baseType.asArray(dimensions);
    }

    private ZType resolveBaseType(ArrayCreationContext ctx) {
        if (ctx.INT() != null) return ZType.INT;
        if (ctx.DOUBLE() != null) return ZType.DOUBLE;
        if (ctx.CHAR() != null) return ZType.CHAR;
        if (ctx.BOOLEAN() != null) return ZType.BOOLEAN;
        if (ctx.STRING() != null) return ZType.STRING;

        if (ctx.IDENTIFIER() != null) {
            String className = ctx.IDENTIFIER().getText();
            if (className.equals(classSymbolTable.getClassName())) {
                return classSymbolTable.getOwnType();
            }
            if (registry != null && registry.contains(className)) {
                return ZType.ofClass(className);
            }
            errors.report("Unknown class '" + className + "'; no class named '" + className + "' is declared or registered", ctx);
            return ZType.UNKNOWN;
        }

        return ZType.UNKNOWN;
    }
}
