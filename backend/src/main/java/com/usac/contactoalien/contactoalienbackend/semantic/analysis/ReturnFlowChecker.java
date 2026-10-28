package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.*;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
Reachability of the statements of a method body, in the way Java defines it: a statement after
return, break or continue is unreachable, and a method with a result must not be able to reach
the end of its body. A loop whose condition is the literal true only ends through a break.
*/
final class ReturnFlowChecker {

    private final ErrorReporter errors;

    ReturnFlowChecker(ErrorReporter errors) {
        this.errors = errors;
    }

    void check(BlockContext body, boolean needsValue) {
        boolean completes = sequence(body.statement());
        if (needsValue && completes) {
            errors.report("Missing return statement; every path of the method must return a value", body.RBRACE().getSymbol());
        }
    }

    private boolean sequence(List<StatementContext> statements) {
        for (int i = 0; i < statements.size(); i++) {
            if (!statement(statements.get(i))) {
                if (i + 1 < statements.size()) {
                    errors.report("Unreachable code: the previous statement never lets execution continue", statements.get(i + 1));
                }
                return false;
            }
        }
        return true;
    }

    private boolean statement(StatementContext ctx) {
        if (ctx.returnStatement() != null || ctx.breakStatement() != null || ctx.continueStatement() != null) {
            return false;
        }
        if (ctx.block() != null) {
            return sequence(ctx.block().statement());
        }
        if (ctx.ifStatement() != null) {
            return ifStatement(ctx.ifStatement());
        }
        if (ctx.whileStatement() != null) {
            WhileStatementContext loop = ctx.whileStatement();
            nested(loop.statementOrBlock());
            return !isTrue(loop.expression()) || contains(loop.statementOrBlock(), BreakStatementContext.class);
        }
        if (ctx.doWhileStatement() != null) {
            DoWhileStatementContext loop = ctx.doWhileStatement();
            boolean body = nested(loop.statementOrBlock()) || contains(loop.statementOrBlock(), ContinueStatementContext.class);
            return (body && !isTrue(loop.expression())) || contains(loop.statementOrBlock(), BreakStatementContext.class);
        }
        if (ctx.forStatement() != null) {
            ForStatementContext loop = ctx.forStatement();
            nested(loop.statementOrBlock());
            boolean endless = loop.expression() == null || isTrue(loop.expression());
            return !endless || contains(loop.statementOrBlock(), BreakStatementContext.class);
        }
        if (ctx.switchStatement() != null) {
            return switchStatement(ctx.switchStatement());
        }
        return true;
    }

    private boolean ifStatement(IfStatementContext ctx) {
        boolean thenCompletes = nested(ctx.statementOrBlock(0));
        if (ctx.statementOrBlock().size() == 1) {
            return true;
        }
        boolean elseCompletes = nested(ctx.statementOrBlock(1));
        return thenCompletes || elseCompletes;
    }

    // A switch can finish when no default exists, when a break leaves it or when its last group runs off the end.
    private boolean switchStatement(SwitchStatementContext ctx) {
        boolean hasDefault = false;
        boolean lastCompletes = true;
        for (SwitchLabelContext label : ctx.switchLabel()) {
            hasDefault |= label.DEFAULT() != null;
            lastCompletes = sequence(label.statement());
        }
        return !hasDefault || lastCompletes || contains(ctx, BreakStatementContext.class);
    }

    private boolean nested(StatementOrBlockContext ctx) {
        if (ctx.block() != null) {
            return sequence(ctx.block().statement());
        }
        return statement(ctx.statement());
    }

    private static boolean isTrue(ExpressionContext ctx) {
        return ctx != null && ctx.getText().equals("true");
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
