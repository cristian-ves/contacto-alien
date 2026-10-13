package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigTypeCompatibility;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

// Analyzes maiorSection (statement+) against the single global PigSymbolTable already populated
// from variabilesSection. There are no additional nested scopes for si/dum/facere/per blocks,
// or for a for-loop's own counter declaration (see PigSymbolTable's own note): the grammar gives
// Pig Latin one flat namespace for the whole program, and nothing in the reference file (or the
// task's own description) calls for shadowing or block-local redeclaration, so introducing scope
// nesting here would add complexity without a concrete requirement driving it.
//
// ifStatement has several statement* blocks (the 'si' block, plus one per 'aliter'), each with
// its own optional condition; the auto-generated flat accessors would merge every block's
// statements and every aliter's condition into single lists that lose the block boundaries, so
// analyzeIf/analyzeFor walk the raw child sequence instead, using expectText as a fail-fast
// sanity check on the shape the grammar guarantees once parsing has already succeeded.
public final class PigStatementAnalyzer {

    private final PigImportResult importResult;
    private final PigSymbolTable symbolTable;
    private final PigErrorReporter reporter;
    private final PigExpressionTypeResolver expressionResolver;

    public PigStatementAnalyzer(PigImportResult importResult, PigSymbolTable symbolTable,
                                PigErrorReporter reporter, PigExpressionTypeResolver expressionResolver) {
        this.importResult = importResult;
        this.symbolTable = symbolTable;
        this.reporter = reporter;
        this.expressionResolver = expressionResolver;
    }

    public void analyze(PigParser.MaiorSectionContext ctx) {
        analyzeStatements(ctx.statement());
    }

    private void analyzeStatements(List<PigParser.StatementContext> statements) {
        for (PigParser.StatementContext stmt : statements) analyzeStatement(stmt);
    }

    private void analyzeStatement(PigParser.StatementContext ctx) {
        if (ctx.assignmentStatement() != null) analyzeAssignment(ctx.assignmentStatement());
        else if (ctx.readStatement() != null) analyzeRead(ctx.readStatement());
        else if (ctx.printStatement() != null) analyzePrint(ctx.printStatement());
        else if (ctx.ifStatement() != null) analyzeIf(ctx.ifStatement());
        else if (ctx.whileStatement() != null) analyzeWhile(ctx.whileStatement());
        else if (ctx.doWhileStatement() != null) analyzeDoWhile(ctx.doWhileStatement());
        else if (ctx.forStatement() != null) analyzeFor(ctx.forStatement());
        else if (ctx.expressionStatement() != null) analyzeExpressionStatement(ctx.expressionStatement());
        // PERGE SEMI / INTERRUMPE SEMI: no semantic validation, same decision as Zetariano/Y?.
    }

    private void analyzeAssignment(PigParser.AssignmentStatementContext ctx) {
        PigType targetType = resolveAssignmentTarget(ctx.assignmentTarget());
        PigType valueType = expressionResolver.resolve(ctx.expression());
        if (!PigTypeCompatibility.isAssignable(targetType, valueType)) {
            reporter.report("cannot assign a value of type " + valueType + " to a target of type " + targetType, ctx);
        }
    }

    private PigType resolveAssignmentTarget(PigParser.AssignmentTargetContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String name = ctx.IDENTIFIER().getText();
            PigVariableSymbol variable = symbolTable.resolve(name);
            if (variable == null) {
                reporter.report("variable '" + name + "' is not declared", ctx);
                return PigType.UNKNOWN;
            }
            return variable.getType();
        }
        return expressionResolver.resolvePostfixExpression(ctx.postfixExpression());
    }

    private void analyzeRead(PigParser.ReadStatementContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        if (symbolTable.resolve(name) == null) {
            reporter.report("variable '" + name + "' is not declared", ctx);
        }
        // The type of the value read is resolved at runtime/code generation, not here.
    }

    private void analyzePrint(PigParser.PrintStatementContext ctx) {
        for (PigParser.ExpressionContext expr : ctx.expression()) {
            expressionResolver.resolve(expr);
        }
    }

    private void analyzeExpressionStatement(PigParser.ExpressionStatementContext ctx) {
        expressionResolver.resolvePostfixExpression(ctx.postfixExpression());
    }

    private void analyzeWhile(PigParser.WhileStatementContext ctx) {
        checkConditionIsBool(ctx.expression(), ctx);
        analyzeStatements(ctx.statement());
    }

    private void analyzeDoWhile(PigParser.DoWhileStatementContext ctx) {
        analyzeStatements(ctx.statement());
        checkConditionIsBool(ctx.expression(), ctx);
    }

    private void checkConditionIsBool(PigParser.ExpressionContext conditionCtx, ParserRuleContext reportCtx) {
        PigType conditionType = expressionResolver.resolve(conditionCtx);
        if (conditionType.getKind() != PigType.Kind.BOOL && !conditionType.isUnknown()) {
            reporter.report("condition must be of type bool, found " + conditionType, reportCtx);
        }
    }

    private void analyzeIf(PigParser.IfStatementContext ctx) {
        int i = 0;
        i = expectText(ctx, i, "si");
        i = expectText(ctx, i, "(");
        checkConditionIsBool((PigParser.ExpressionContext) ctx.getChild(i), ctx);
        i++;
        i = expectText(ctx, i, ")");
        i = expectText(ctx, i, "{");
        i = analyzeStatementBlockFrom(ctx, i);
        i = expectText(ctx, i, "}");

        while (i < ctx.getChildCount() && ctx.getChild(i).getText().equals("aliter")) {
            i++; // consume ALITER
            if (i < ctx.getChildCount() && ctx.getChild(i).getText().equals("(")) {
                i++; // consume LPAREN
                checkConditionIsBool((PigParser.ExpressionContext) ctx.getChild(i), ctx);
                i++;
                i = expectText(ctx, i, ")");
            }
            i = expectText(ctx, i, "{");
            i = analyzeStatementBlockFrom(ctx, i);
            i = expectText(ctx, i, "}");
        }
        // FINIS SEMI follow; nothing further to check.
    }

    private void analyzeFor(PigParser.ForStatementContext ctx) {
        int i = 0;
        i = expectText(ctx, i, "per");
        i = expectText(ctx, i, "(");

        if (!ctx.getChild(i).getText().equals(";")) {
            analyzeForInit((PigParser.ForInitContext) ctx.getChild(i));
            i++;
        }
        i = expectText(ctx, i, ";");

        if (!ctx.getChild(i).getText().equals(";")) {
            checkConditionIsBool((PigParser.ExpressionContext) ctx.getChild(i), ctx);
            i++;
        }
        i = expectText(ctx, i, ";");

        if (!ctx.getChild(i).getText().equals(")")) {
            expressionResolver.resolve((PigParser.ExpressionContext) ctx.getChild(i));
            i++;
        }
        i = expectText(ctx, i, ")");
        i = expectText(ctx, i, "{");
        analyzeStatementBlockFrom(ctx, i);
        // trailing RBRACE not consumed further; nothing left to validate.
    }

    private void analyzeForInit(PigParser.ForInitContext ctx) {
        if (ctx.ESTO() != null) {
            String name = ctx.IDENTIFIER().getText();
            PigType declaredType = PigTypeMapper.mapTipoBasico(ctx.tipoBasico());
            PigType initType = expressionResolver.resolve(ctx.expression());
            if (!PigTypeCompatibility.isAssignable(declaredType, initType)) {
                reporter.report("cannot initialize variable '" + name + "' of type " + declaredType
                        + " with a value of type " + initType, ctx);
            }
            if (!symbolTable.declare(new PigVariableSymbol(name, declaredType))) {
                reporter.report("variable '" + name + "' is already declared", ctx);
            }
        } else {
            expressionResolver.resolve(ctx.expression());
        }
    }

    private int analyzeStatementBlockFrom(ParserRuleContext ctx, int startIndex) {
        int i = startIndex;
        while (i < ctx.getChildCount() && ctx.getChild(i) instanceof PigParser.StatementContext statementCtx) {
            analyzeStatement(statementCtx);
            i++;
        }
        return i;
    }

    // Advances past one expected literal token. The grammar guarantees it is there once parsing
    // has already succeeded without syntax errors; this only walks the tree and fails loudly if
    // that assumption is ever wrong, rather than silently misreading the structure.
    private int expectText(ParserRuleContext ctx, int index, String expectedText) {
        String actual = ctx.getChild(index).getText();
        if (!actual.equals(expectedText)) {
            throw new IllegalStateException("Expected '" + expectedText + "' but found '" + actual
                    + "' while walking " + ctx.getClass().getSimpleName());
        }
        return index + 1;
    }
}