package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.*;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
Reachability of the statements of a Y? function: a statement after retornar, romper or continuar
is unreachable, and a function with a result must not be able to reach the end of its body.
A loop whose condition is the literal verdadero only ends through a romper.
*/
final class YReturnFlowChecker {

    private final YErrorReporter errors;

    YReturnFlowChecker(YErrorReporter errors) {
        this.errors = errors;
    }

    void check(FunctionDeclarationContext function, boolean needsValue) {
        boolean completes = sequence(function.statement());
        if (needsValue && completes) {
            errors.report("Missing return: function '" + function.IDENTIFIER().getText()
                    + "' must return a value on every path", function.IDENTIFIER().getSymbol());
        }
    }

    private boolean sequence(List<StatementContext> statements) {
        for (int i = 0; i < statements.size(); i++) {
            if (!statement(statements.get(i))) {
                if (i + 1 < statements.size() && endsBlock(statements.get(i))) {
                    errors.report("Unreachable code: the previous statement never lets execution continue", statements.get(i + 1));
                }
                return false;
            }
        }
        return true;
    }

    // Only a direct retornar, romper or continuar makes the next statement an error; whether a compound
    // statement can finish is still used to decide if the function reaches its end.
    private static boolean endsBlock(StatementContext ctx) {
        return ctx instanceof ReturnStatementContext || ctx instanceof BreakStatementContext
                || ctx instanceof ContinueStatementContext;
    }

    private boolean statement(StatementContext ctx) {
        if (endsBlock(ctx)) {
            return false;
        }
        if (ctx instanceof IfStmtContext s) {
            return ifStatement(s.ifStatement());
        }
        if (ctx instanceof WhileStmtContext s) {
            WhileStatementContext loop = s.whileStatement();
            sequence(loop.statement());
            return !isTrue(loop.expression()) || contains(loop, BreakStatementContext.class);
        }
        if (ctx instanceof DoWhileStmtContext s) {
            DoWhileStatementContext loop = s.doWhileStatement();
            boolean body = sequence(loop.statement()) || contains(loop, ContinueStatementContext.class);
            return (body && !isTrue(loop.expression())) || contains(loop, BreakStatementContext.class);
        }
        if (ctx instanceof ForStmtContext s) {
            ForStatementContext loop = s.forStatement();
            sequence(loop.statement());
            boolean endless = loop.expression() == null || isTrue(loop.expression());
            return !endless || contains(loop, BreakStatementContext.class);
        }
        if (ctx instanceof SwitchStmtContext s) {
            return switchStatement(s.switchStatement());
        }
        return true;
    }

    private boolean ifStatement(IfStatementContext ctx) {
        boolean completes = sequence(ctx.statement());
        for (ElseIfClauseContext clause : ctx.elseIfClause()) {
            completes |= sequence(clause.statement());
        }
        if (ctx.elseClause() == null) {
            return true;
        }
        return sequence(ctx.elseClause().statement()) || completes;
    }

    // An elegir can finish when no siempre exists, when a romper leaves it or when its last group runs off the end.
    private boolean switchStatement(SwitchStatementContext ctx) {
        boolean lastCompletes = true;
        for (CaseClauseContext clause : ctx.caseClause()) {
            lastCompletes = sequence(clause.statement());
        }
        if (ctx.defaultClause() == null) {
            return true;
        }
        lastCompletes = sequence(ctx.defaultClause().statement());
        return lastCompletes || contains(ctx, BreakStatementContext.class);
    }

    private static boolean isTrue(ExpressionContext ctx) {
        return ctx != null && ctx.getText().equals("verdadero");
    }

    // True when a break or continue of the given kind can leave this statement; nested loops and switches keep their own.
    private static boolean contains(ParseTree tree, Class<? extends ParseTree> kind) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (kind.isInstance(child)) {
                return true;
            }
            boolean ownsJumps = child instanceof WhileStatementContext || child instanceof DoWhileStatementContext
                    || child instanceof ForStatementContext || child instanceof SwitchStatementContext;
            if (!ownsJumps && contains(child, kind)) {
                return true;
            }
        }
        return false;
    }
}
