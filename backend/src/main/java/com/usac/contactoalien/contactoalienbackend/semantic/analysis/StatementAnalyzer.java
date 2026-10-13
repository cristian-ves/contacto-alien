package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.*;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParserBaseVisitor;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.Scope;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.VariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.types.TypeCompatibility;
import com.usac.contactoalien.contactoalienbackend.semantic.types.TypeResolver;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

// Second pass: walks a method/constructor body via the ANTLR visitor pattern, managing nested
// block scopes and delegating expression type-checking to ExpressionTypeResolver. Local variable
// types may reference any class already registered in the CompilationUnitRegistry, not only the
// class under analysis.
public final class StatementAnalyzer extends ZetarianoParserBaseVisitor<Void> {

    private final ClassSymbolTable classSymbolTable;
    private final CompilationUnitRegistry registry;
    private final ErrorReporter errors;
    private final ExpressionTypeResolver expressionResolver;
    private Scope currentScope;
    private ZType currentReturnType;

    public StatementAnalyzer(ClassSymbolTable classSymbolTable, CompilationUnitRegistry registry, ErrorReporter errors) {
        this.classSymbolTable = classSymbolTable;
        this.registry = registry;
        this.errors = errors;
        this.expressionResolver = new ExpressionTypeResolver(classSymbolTable, registry, errors);
    }

    // returnType should be ZType.VOID for both void methods and constructors (constructors have
    // no return type in this grammar, so 'return <value>;' inside one is treated as an error,
    // exactly like inside a void method; 'return;' with no value is allowed).
    public void analyzeBody(BlockContext body, Scope parameterScope, ZType returnType) {
        this.currentReturnType = returnType;
        this.currentScope = parameterScope;
        visit(body);
    }

    @Override
    public Void visitBlock(BlockContext ctx) {
        Scope saved = currentScope;
        currentScope = new Scope(saved);
        for (StatementContext s : ctx.statement()) {
            visit(s);
        }
        currentScope = saved;
        return null;
    }

    @Override
    public Void visitVariableDeclarationStatement(VariableDeclarationStatementContext ctx) {
        ZType type = TypeResolver.resolve(ctx.type(), classSymbolTable, registry, errors);
        String name = ctx.IDENTIFIER().getText();
        if (!currentScope.declare(new VariableSymbol(name, type))) {
            errors.report("Variable '" + name + "' is already declared in this scope", ctx);
        }
        if (ctx.expression() != null) {
            expressionResolver.setScope(currentScope);
            ZType valueType = expressionResolver.resolve(ctx.expression());
            if (!TypeCompatibility.isAssignable(type, valueType)) {
                errors.report("Cannot initialize variable '" + name + "' of type '" + type + "' with value of type '" + valueType + "'", ctx);
            }
        }
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementContext ctx) {
        expressionResolver.setScope(currentScope);
        expressionResolver.resolve(ctx.expression());
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatementContext ctx) {
        expressionResolver.setScope(currentScope);
        ZType condType = expressionResolver.resolve(ctx.expression());
        if (!TypeCompatibility.isValidLogicalOperand(condType)) {
            errors.report("'if' condition must be boolean, found '" + condType + "'", ctx.expression());
        }
        for (StatementOrBlockContext s : ctx.statementOrBlock()) {
            visit(s);
        }
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatementContext ctx) {
        expressionResolver.setScope(currentScope);
        ZType condType = expressionResolver.resolve(ctx.expression());
        if (!TypeCompatibility.isValidLogicalOperand(condType)) {
            errors.report("'while' condition must be boolean, found '" + condType + "'", ctx.expression());
        }
        visit(ctx.statementOrBlock());
        return null;
    }

    @Override
    public Void visitDoWhileStatement(DoWhileStatementContext ctx) {
        visit(ctx.statementOrBlock());
        expressionResolver.setScope(currentScope);
        ZType condType = expressionResolver.resolve(ctx.expression());
        if (!TypeCompatibility.isValidLogicalOperand(condType)) {
            errors.report("'do-while' condition must be boolean, found '" + condType + "'", ctx.expression());
        }
        return null;
    }

    @Override
    public Void visitForStatement(ForStatementContext ctx) {
        // the header (init/condition/update) and the body share one nested scope, so a variable
        // declared in the init clause is visible in the loop body
        Scope saved = currentScope;
        currentScope = new Scope(saved);
        if (ctx.forInit() != null) {
            analyzeForInit(ctx.forInit());
        }
        if (ctx.expression() != null) {
            expressionResolver.setScope(currentScope);
            ZType condType = expressionResolver.resolve(ctx.expression());
            if (!TypeCompatibility.isValidLogicalOperand(condType)) {
                errors.report("'for' condition must be boolean, found '" + condType + "'", ctx.expression());
            }
        }
        if (ctx.forUpdate() != null) {
            expressionResolver.setScope(currentScope);
            for (ExpressionContext e : ctx.forUpdate().expression()) {
                expressionResolver.resolve(e);
            }
        }
        visit(ctx.statementOrBlock());
        currentScope = saved;
        return null;
    }

    private void analyzeForInit(ForInitContext ctx) {
        if (ctx.type() != null) {
            ZType type = TypeResolver.resolve(ctx.type(), classSymbolTable, registry, errors);
            String name = ctx.IDENTIFIER().getText();
            if (!currentScope.declare(new VariableSymbol(name, type))) {
                errors.report("Variable '" + name + "' is already declared in this scope", ctx);
            }
            if (ctx.expression() != null) {
                expressionResolver.setScope(currentScope);
                ZType valueType = expressionResolver.resolve(ctx.expression());
                if (!TypeCompatibility.isAssignable(type, valueType)) {
                    errors.report("Cannot initialize variable '" + name + "' of type '" + type + "' with value of type '" + valueType + "'", ctx);
                }
            }
        } else if (ctx.expression() != null) {
            expressionResolver.setScope(currentScope);
            expressionResolver.resolve(ctx.expression());
        }
    }

    @Override
    public Void visitSwitchStatement(SwitchStatementContext ctx) {
        expressionResolver.setScope(currentScope);
        expressionResolver.resolve(ctx.expression());
        for (SwitchLabelContext label : ctx.switchLabel()) {
            Scope saved = currentScope;
            currentScope = new Scope(saved);
            for (StatementContext s : label.statement()) {
                visit(s);
            }
            currentScope = saved;
        }
        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatementContext ctx) {
        if (ctx.expression() == null) {
            if (currentReturnType.getKind() != ZType.Kind.VOID) {
                errors.report("Missing return value; method must return '" + currentReturnType + "'", ctx);
            }
            return null;
        }
        expressionResolver.setScope(currentScope);
        ZType valueType = expressionResolver.resolve(ctx.expression());
        if (currentReturnType.getKind() == ZType.Kind.VOID) {
            errors.report("Cannot return a value from a void method or constructor", ctx);
        } else if (!TypeCompatibility.isAssignable(currentReturnType, valueType)) {
            errors.report("Cannot return value of type '" + valueType + "'; expected '" + currentReturnType + "'", ctx);
        }
        return null;
    }

    @Override
    public Void visitPrintlnStatement(PrintlnStatementContext ctx) {
        resolveArgumentsForSideEffect(ctx.argumentList());
        return null;
    }

    @Override
    public Void visitPrintStatement(PrintStatementContext ctx) {
        resolveArgumentsForSideEffect(ctx.argumentList());
        return null;
    }

    private void resolveArgumentsForSideEffect(ArgumentListContext ctx) {
        if (ctx == null) return;
        expressionResolver.setScope(currentScope);
        for (ExpressionContext e : ctx.expression()) {
            expressionResolver.resolve(e);
        }
    }
}