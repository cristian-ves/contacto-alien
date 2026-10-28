package com.usac.contactoalien.contactoalienbackend.codegen.gen.z;

import com.usac.contactoalien.contactoalienbackend.grammar.ReadlnForms;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.ControlFlow;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LocalScope;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;

import java.util.ArrayList;
import java.util.List;

// Translates the statements of a Zetariano method or constructor body; block layout comes from ControlFlow.
final class ZStatementGen {

    private final GenContext gen;
    private final ZExpressionGen exprs;
    private final GenType returnType;

    ZStatementGen(GenContext gen, ZExpressionGen exprs, GenType returnType) {
        this.gen = gen;
        this.exprs = exprs;
        this.returnType = returnType;
    }

    void block(ZetarianoParser.BlockContext ctx) {
        gen.scope().push();
        for (ZetarianoParser.StatementContext statement : ctx.statement()) {
            statement(statement);
        }
        gen.scope().pop();
    }

    private void statementOrBlock(ZetarianoParser.StatementOrBlockContext ctx) {
        if (ctx.block() != null) {
            block(ctx.block());
        } else {
            gen.scope().push();
            statement(ctx.statement());
            gen.scope().pop();
        }
    }

    private void statement(ZetarianoParser.StatementContext ctx) {
        if (ctx.variableDeclarationStatement() != null) {
            declaration(ctx.variableDeclarationStatement());
        } else if (ctx.expressionStatement() != null) {
            exprs.expression(ctx.expressionStatement().expression());
        } else if (ctx.printlnStatement() != null) {
            print(ctx, ctx.printlnStatement().argumentList(), true);
        } else if (ctx.printStatement() != null) {
            print(ctx, ctx.printStatement().argumentList(), false);
        } else if (ctx.ifStatement() != null) {
            ifStatement(ctx.ifStatement());
        } else if (ctx.whileStatement() != null) {
            ZetarianoParser.WhileStatementContext loop = ctx.whileStatement();
            gen.flow().whileLoop(() -> exprs.expression(loop.expression()), () -> statementOrBlock(loop.statementOrBlock()));
        } else if (ctx.doWhileStatement() != null) {
            ZetarianoParser.DoWhileStatementContext loop = ctx.doWhileStatement();
            gen.flow().doWhile(() -> statementOrBlock(loop.statementOrBlock()), () -> exprs.expression(loop.expression()));
        } else if (ctx.forStatement() != null) {
            forStatement(ctx.forStatement());
        } else if (ctx.returnStatement() != null) {
            returnStatement(ctx.returnStatement());
        } else if (ctx.breakStatement() != null) {
            gen.flow().breakLoop(ctx);
        } else if (ctx.continueStatement() != null) {
            gen.flow().continueLoop(ctx);
        } else if (ctx.switchStatement() != null) {
            switchStatement(ctx.switchStatement());
        } else {
            block(ctx.block());
        }
    }

    private void declaration(ZetarianoParser.VariableDeclarationStatementContext ctx) {
        declare(ZTypeMapper.map(ctx.type()), ctx.IDENTIFIER().getText(), ctx.variableInitializer());
    }

    // The initializer is translated before the name enters the scope, and converted to the declared type.
    private void declare(GenType type, String name, ZetarianoParser.VariableInitializerContext initializer) {
        boolean readsLine = initializer != null && initializer.expression() != null
                && ReadlnForms.isBareCall(initializer.expression()) && gen.ops().isReadable(type);
        String initial = initializer == null ? type.defaultOperand()
                : readsLine ? gen.ops().read(type).operand() : exprs.arrays().initializer(initializer, type).operand();
        LocalScope.Variable variable = gen.scope().declareLocal(name, type);
        gen.quads().assign(variable.operand(), initial);
    }

    // println(a, b) prints its arguments in order and ends the line; print omits the line end.
    private void print(ZetarianoParser.StatementContext at, ZetarianoParser.ArgumentListContext arguments, boolean newline) {
        for (ExprValue argument : exprs.arguments(arguments)) {
            gen.ops().print(argument, at);
        }
        if (newline) {
            gen.ops().printNewline();
        }
    }

    private void returnStatement(ZetarianoParser.ReturnStatementContext ctx) {
        String value = ctx.expression() == null ? null
                : gen.ops().coerce(exprs.expression(ctx.expression()), returnType).operand();
        gen.quads().emit(QuadOp.RETURN, value, null, null);
    }

    private void ifStatement(ZetarianoParser.IfStatementContext ctx) {
        List<ZetarianoParser.StatementOrBlockContext> branches = ctx.statementOrBlock();
        gen.flow().ifElse(() -> exprs.expression(ctx.expression()), () -> statementOrBlock(branches.get(0)),
                branches.size() > 1 ? () -> statementOrBlock(branches.get(1)) : null);
    }

    private void forStatement(ZetarianoParser.ForStatementContext ctx) {
        ZetarianoParser.ForInitContext init = ctx.forInit();
        ZetarianoParser.ForUpdateContext update = ctx.forUpdate();
        gen.scope().push();
        gen.flow().forLoop(
                init == null ? null : () -> forInit(init),
                ctx.expression() == null ? null : () -> exprs.expression(ctx.expression()),
                update == null ? null : () -> update.expression().forEach(exprs::expression),
                () -> statementOrBlock(ctx.statementOrBlock()));
        gen.scope().pop();
    }

    private void forInit(ZetarianoParser.ForInitContext ctx) {
        if (ctx.type() == null) {
            exprs.expression(ctx.expression());
            return;
        }
        GenType type = ZTypeMapper.map(ctx.type());
        String initial = ctx.expression() == null ? type.defaultOperand()
                : gen.ops().coerce(exprs.expression(ctx.expression()), type).operand();
        LocalScope.Variable variable = gen.scope().declareLocal(ctx.IDENTIFIER().getText(), type);
        gen.quads().assign(variable.operand(), initial);
    }

    // The whole switch is one scope, so a declaration in one case is visible in the cases after it.
    private void switchStatement(ZetarianoParser.SwitchStatementContext ctx) {
        List<ControlFlow.SwitchArm> arms = new ArrayList<>();
        for (ZetarianoParser.SwitchLabelContext label : ctx.switchLabel()) {
            arms.add(new ControlFlow.SwitchArm(label.CASE() == null ? null : () -> exprs.caseLabel(label),
                    () -> label.statement().forEach(this::statement)));
        }
        ExprValue subject = exprs.expression(ctx.expression());
        gen.scope().push();
        gen.flow().switchOn(subject, arms, ctx);
        gen.scope().pop();
    }
}
