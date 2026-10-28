package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigTypeCompatibility;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

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
// analyzeIf walks the raw child sequence instead, using expectText as a fail-fast sanity check
// on the shape the grammar guarantees once parsing has already succeeded. forStatement uses typed
// accessors and compares expression token positions against semicolons.
public final class PigStatementAnalyzer {

    private final PigImportResult importResult;
    private final PigSymbolTable symbolTable;
    private final PigErrorReporter reporter;
    private final PigExpressionTypeResolver expressionResolver;
    private int loopDepth;

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
        else if (loopDepth == 0 && ctx.PERGE() != null) {
            reporter.report("'perge' can only be used inside a loop (dum, facere or per)", ctx);
        } else if (loopDepth == 0 && ctx.INTERRUMPE() != null) {
            reporter.report("'interrumpe' can only be used inside a loop (dum, facere or per)", ctx);
        }
    }

    private void analyzeAssignment(PigParser.AssignmentStatementContext ctx) {
        PigType targetType = resolveAssignmentTarget(ctx.assignmentTarget());
        PigType valueType = expressionResolver.resolve(ctx.expression());
        if (!PigTypeCompatibility.isAssignable(targetType, valueType, importResult.getClassRegistry())) {
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
        if (!isAssignablePlace(ctx.postfixExpression())) {
            reporter.report("the target of an assignment or a read must be a variable, an object field or a series element", ctx);
            return PigType.UNKNOWN;
        }
        return expressionResolver.resolvePostfixExpression(ctx.postfixExpression());
    }

    // A variable followed by a field access or an index; calls, literals and `novus` cannot be assigned.
    private static boolean isAssignablePlace(PigParser.PostfixExpressionContext ctx) {
        PigParser.PrimaryExpressionContext primary = ctx.primaryExpression();
        List<PigParser.PostfixOpContext> ops = ctx.postfixOp();
        if (primary.IDENTIFIER() == null || primary.NOVUS() != null || ops.isEmpty()) {
            return false;
        }
        PigParser.PostfixOpContext last = ops.get(ops.size() - 1);
        return last.LBRACK() != null || (last.DOT() != null && last.IDENTIFIER() != null);
    }

    private void analyzeRead(PigParser.ReadStatementContext ctx) {
        PigParser.AssignmentTargetContext target = ctx.assignmentTarget();
        PigType type = resolveAssignmentTarget(target);
        if (!type.isUnknown()) {
            new PigCollectionRules(reporter).checkReadable(target.getText(), type, ctx);
        }
    }

    private void analyzePrint(PigParser.PrintStatementContext ctx) {
        for (PigParser.ExpressionContext expr : ctx.expression()) {
            PigType type = expressionResolver.resolve(expr);
            if (type.getKind() == PigType.Kind.ARRAY || type.getKind() == PigType.Kind.OBJECT
                    || type.getKind() == PigType.Kind.VOID) {
                reporter.report("cannot print a value of type " + type + "; print its fields or elements one by one", expr);
            }
        }
    }

    private void analyzeExpressionStatement(PigParser.ExpressionStatementContext ctx) {
        expressionResolver.resolvePostfixExpression(ctx.postfixExpression());
    }

    private void analyzeWhile(PigParser.WhileStatementContext ctx) {
        checkConditionIsBool(ctx.expression(), ctx);
        loopDepth++;
        analyzeStatements(ctx.statement());
        loopDepth--;
    }

    private void analyzeDoWhile(PigParser.DoWhileStatementContext ctx) {
        loopDepth++;
        analyzeStatements(ctx.statement());
        loopDepth--;
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
        symbolTable.enterScope();
        try {
            if (ctx.forInit() != null) {
                analyzeForInit(ctx.forInit());
            }

            List<TerminalNode> semis = ctx.SEMI();
            if (semis.size() >= 2) {
                int firstSemiIndex = semis.get(0).getSymbol().getTokenIndex();
                int secondSemiIndex = semis.get(1).getSymbol().getTokenIndex();

                for (PigParser.ExpressionContext expr : ctx.expression()) {
                    int exprStartIndex = expr.getStart().getTokenIndex();
                    if (exprStartIndex > firstSemiIndex && exprStartIndex < secondSemiIndex) {
                        checkConditionIsBool(expr, ctx);
                    } else if (exprStartIndex > secondSemiIndex) {
                        expressionResolver.resolve(expr);
                    }
                }
            }

            loopDepth++;
            for (PigParser.StatementContext stmt : ctx.statement()) {
                analyzeStatement(stmt);
            }
            loopDepth--;
        } finally {
            symbolTable.exitScope();
        }
    }

    private void analyzeForInit(PigParser.ForInitContext ctx) {
        if (ctx.ESTO() != null) {
            String name = ctx.IDENTIFIER().getText();
            PigType declaredType = PigTypeMapper.mapTipoBasico(ctx.tipoBasico());
            PigType initType = expressionResolver.resolve(ctx.expression());
            if (!PigTypeCompatibility.isAssignable(declaredType, initType, importResult.getClassRegistry())) {
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