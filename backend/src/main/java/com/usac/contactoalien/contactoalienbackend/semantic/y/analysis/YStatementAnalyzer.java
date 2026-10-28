package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.*;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParserBaseVisitor;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YFunctionRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YScope;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeCompatibility;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeResolver;

import java.util.List;

// Walks a function body via the ANTLR visitor pattern, managing nested scopes (one new scope
// per loop/conditional body, matching this grammar's flat `statement+` bodies with no
// separate `block` rule) and delegating expression type-checking to YExpressionTypeResolver.
public final class YStatementAnalyzer extends YParserBaseVisitor<Void> {

    private final YStructRegistry structRegistry;
    private final YErrorReporter errors;
    private final YExpressionTypeResolver expressionResolver;
    private YScope currentScope;
    private YType currentReturnType;
    private final YCollectionRules collections;
    private int loopDepth;
    private int switchDepth;

    public YStatementAnalyzer(YStructRegistry structRegistry, YFunctionRegistry functionRegistry, YErrorReporter errors) {
        this.structRegistry = structRegistry;
        this.errors = errors;
        this.collections = new YCollectionRules(errors);
        this.expressionResolver = new YExpressionTypeResolver(structRegistry, functionRegistry, errors);
    }

    // returnType is YType.VOID for a function with no declared '-> tipo'.
    public void analyzeBody(List<StatementContext> statements, YScope parameterScope, YType returnType) {
        this.currentReturnType = returnType;
        this.currentScope = parameterScope;
        for (StatementContext s : statements) {
            visit(s);
        }
    }

    @Override
    public Void visitVariableDeclarationStatement(VariableDeclarationStatementContext ctx) {
        declareVariable(ctx.variableDeclaration());
        return null;
    }

    private void declareVariable(VariableDeclarationContext ctx) {
        YType base = YTypeResolver.resolve(ctx.type(), structRegistry, errors);
        int dimensions = ctx.LBRACK().size();
        YType type = dimensions == 0 ? base : base.asArray(dimensions);
        String name = ctx.IDENTIFIER().getText();
        int length = dimensions == 1 ? IntegerLiterals.length(ctx.INT_LITERAL(0).getText()) : -1;
        if (!currentScope.declare(new YVariableSymbol(name, type, length))) {
            errors.report("Variable '" + name + "' is already declared in this scope", ctx);
        }
        collections.checkLength(ctx);
        PrimaryContext list = ctx.ASSIGN() == null ? null : YPlaceRules.descend(ctx.expression(), PrimaryContext.class);
        if (dimensions > 1 && ctx.ASSIGN() != null) {
            errors.report("Array '" + name + "' has more than one dimension and cannot be initialized with a list", ctx);
        } else if (dimensions == 1 && list != null && list.LBRACE() != null) {
            expressionResolver.setScope(currentScope);
            List<YType> values = list.expression().stream().map(expressionResolver::resolve).toList();
            collections.checkInitializer(ctx, base, values);
        } else if (list != null && list.LBRACE() != null && base.getKind() == YType.Kind.STRUCT && !type.isArray()) {
            expressionResolver.setScope(currentScope);
            List<YType> values = list.expression().stream().map(expressionResolver::resolve).toList();
            YStructSymbol struct = structRegistry.resolve(base.getStructName());
            if (struct != null) {
                collections.checkStructList(struct, values, ctx);
            }
        } else if (ctx.ASSIGN() != null) {
            expressionResolver.setScope(currentScope);
            YType valueType = expressionResolver.resolve(ctx.expression());
            if (!YTypeCompatibility.isAssignable(type, valueType)) {
                errors.report("Cannot initialize variable '" + name + "' of type '" + type + "' with value of type '" + valueType + "'", ctx);
            }
        }
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementContext ctx) {
        expressionResolver.setScope(currentScope);
        expressionResolver.resolve(ctx.expression());
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatementContext ctx) {
        checkCondition(ctx.expression(), "si");
        visitStatementsInNewScope(ctx.statement());
        for (ElseIfClauseContext elseIf : ctx.elseIfClause()) {
            visit(elseIf);
        }
        if (ctx.elseClause() != null) {
            visit(ctx.elseClause());
        }
        return null;
    }

    @Override
    public Void visitElseIfClause(ElseIfClauseContext ctx) {
        checkCondition(ctx.expression(), "sino");
        visitStatementsInNewScope(ctx.statement());
        return null;
    }

    @Override
    public Void visitElseClause(ElseClauseContext ctx) {
        visitStatementsInNewScope(ctx.statement());
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatementContext ctx) {
        checkCondition(ctx.expression(), "mientras");
        loopDepth++;
        visitStatementsInNewScope(ctx.statement());
        loopDepth--;
        return null;
    }

    @Override
    public Void visitDoWhileStatement(DoWhileStatementContext ctx) {
        loopDepth++;
        visitStatementsInNewScope(ctx.statement());
        loopDepth--;
        checkCondition(ctx.expression(), "hacer-mientras");
        return null;
    }

    @Override
    public Void visitForStatement(ForStatementContext ctx) {
        // header (init/condition/update) and body share one nested scope
        YScope saved = currentScope;
        currentScope = new YScope(saved);
        if (ctx.forInit() != null) analyzeForInit(ctx.forInit());
        if (ctx.expression() != null) checkCondition(ctx.expression(), "para");
        if (ctx.forUpdate() != null) {
            expressionResolver.setScope(currentScope);
            expressionResolver.resolve(ctx.forUpdate().expression());
        }
        loopDepth++;
        for (StatementContext s : ctx.statement()) visit(s);
        loopDepth--;
        currentScope = saved;
        return null;
    }

    private void analyzeForInit(ForInitContext ctx) {
        if (ctx.type() != null) {
            YType type = YTypeResolver.resolve(ctx.type(), structRegistry, errors);
            String name = ctx.IDENTIFIER().getText();
            if (!currentScope.declare(new YVariableSymbol(name, type))) {
                errors.report("Variable '" + name + "' is already declared in this scope", ctx);
            }
            expressionResolver.setScope(currentScope);
            YType valueType = expressionResolver.resolve(ctx.expression());
            if (!YTypeCompatibility.isAssignable(type, valueType)) {
                errors.report("Cannot initialize variable '" + name + "' of type '" + type + "' with value of type '" + valueType + "'", ctx);
            }
        } else {
            expressionResolver.setScope(currentScope);
            expressionResolver.resolve(ctx.expression());
        }
    }

    @Override
    public Void visitSwitchStatement(SwitchStatementContext ctx) {
        expressionResolver.setScope(currentScope);
        YType subject = expressionResolver.resolve(ctx.expression());
        YSwitchRules rules = new YSwitchRules(errors);
        rules.checkSubject(subject, ctx.expression());
        switchDepth++;
        for (CaseClauseContext c : ctx.caseClause()) {
            expressionResolver.setScope(currentScope);
            rules.checkCase(subject, expressionResolver.resolve(c.expression()), c.expression());
            visitStatementsInNewScope(c.statement());
        }
        if (ctx.defaultClause() != null) {
            visitStatementsInNewScope(ctx.defaultClause().statement());
        }
        switchDepth--;
        return null;
    }

    @Override
    public Void visitBreakStatement(BreakStatementContext ctx) {
        if (loopDepth + switchDepth == 0) {
            errors.report("'romper' can only be used inside a loop or an 'elegir'", ctx);
        }
        return null;
    }

    @Override
    public Void visitContinueStatement(ContinueStatementContext ctx) {
        if (loopDepth == 0) {
            errors.report("'continuar' can only be used inside a loop", ctx);
        }
        return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatementContext ctx) {
        if (ctx.expression() == null) {
            if (currentReturnType.getKind() != YType.Kind.VOID) {
                errors.report("Missing return value; function must return '" + currentReturnType + "'", ctx);
            }
            return null;
        }
        expressionResolver.setScope(currentScope);
        YType valueType = expressionResolver.resolve(ctx.expression());
        if (currentReturnType.getKind() == YType.Kind.VOID) {
            errors.report("Cannot return a value from a function with no declared return type", ctx);
        } else if (!YTypeCompatibility.isAssignable(currentReturnType, valueType)) {
            errors.report("Cannot return value of type '" + valueType + "'; expected '" + currentReturnType + "'", ctx);
        }
        return null;
    }

    private void checkCondition(ExpressionContext expr, String label) {
        expressionResolver.setScope(currentScope);
        YType condType = expressionResolver.resolve(expr);
        if (!YTypeCompatibility.isValidLogicalOperand(condType)) {
            errors.report("'" + label + "' condition must be boolean, found '" + condType + "'", expr);
        }
    }

    private void visitStatementsInNewScope(List<StatementContext> statements) {
        YScope saved = currentScope;
        currentScope = new YScope(saved);
        for (StatementContext s : statements) visit(s);
        currentScope = saved;
    }
}