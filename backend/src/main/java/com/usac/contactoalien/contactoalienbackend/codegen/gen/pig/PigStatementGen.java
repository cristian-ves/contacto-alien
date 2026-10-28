package com.usac.contactoalien.contactoalienbackend.codegen.gen.pig;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.Target;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LocalScope;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
Translates the statements of the MAIOR> section. Structured statements are laid out by
ControlFlow; this class only decides which parts of the parse tree are the condition,
the body, the initializer and so on.
*/
final class PigStatementGen {

    // One `si` or `aliter` arm; the condition is null for a final `aliter { ... }`.
    private static final class Branch {
        private PigParser.ExpressionContext condition;
        private final List<PigParser.StatementContext> body = new ArrayList<>();
    }

    private final GenContext gen;
    private final PigExpressionGen exprs;

    PigStatementGen(GenContext gen, PigExpressionGen exprs) {
        this.gen = gen;
        this.exprs = exprs;
    }

    void statements(List<PigParser.StatementContext> list) {
        for (PigParser.StatementContext statement : list) {
            statement(statement);
        }
    }

    void statement(PigParser.StatementContext ctx) {
        if (ctx.assignmentStatement() != null) {
            assignment(ctx.assignmentStatement());
        } else if (ctx.printStatement() != null) {
            print(ctx.printStatement());
        } else if (ctx.ifStatement() != null) {
            ifStatement(ctx.ifStatement());
        } else if (ctx.whileStatement() != null) {
            PigParser.WhileStatementContext loop = ctx.whileStatement();
            gen.flow().whileLoop(() -> exprs.expression(loop.expression()), () -> statements(loop.statement()));
        } else if (ctx.doWhileStatement() != null) {
            PigParser.DoWhileStatementContext loop = ctx.doWhileStatement();
            gen.flow().doWhile(() -> statements(loop.statement()), () -> exprs.expression(loop.expression()));
        } else if (ctx.forStatement() != null) {
            forStatement(ctx.forStatement());
        } else if (ctx.PERGE() != null) {
            gen.flow().continueLoop(ctx);
        } else if (ctx.INTERRUMPE() != null) {
            gen.flow().breakLoop(ctx);
        } else if (ctx.expressionStatement() != null) {
            exprs.visit(ctx.expressionStatement().postfixExpression());
        } else {
            read(ctx.readStatement());
        }
    }

    private void assignment(PigParser.AssignmentStatementContext ctx) {
        ExprValue value = exprs.expression(ctx.expression());
        PigParser.AssignmentTargetContext target = ctx.assignmentTarget();
        if (target.IDENTIFIER() != null) {
            gen.members().variableTarget(gen.variable(target.IDENTIFIER().getText(), ctx)).write(value);
        } else {
            exprs.assignTo(target.postfixExpression(), value);
        }
    }

    // `x <<` reads a line of standard input and stores it in x (a variable, a field or a series element), converted according to its type.
    private void read(PigParser.ReadStatementContext ctx) {
        PigParser.AssignmentTargetContext written = ctx.assignmentTarget();
        Target target = written.IDENTIFIER() != null
                ? gen.members().variableTarget(gen.variable(written.IDENTIFIER().getText(), ctx))
                : exprs.place(written.postfixExpression());
        GenType.Kind kind = target.type().kind();
        if (kind == GenType.Kind.OBJECT || kind == GenType.Kind.ARRAY) {
            throw GenerationException.unsupported(GenerationMessages.FEATURE_READ, ctx);
        }
        target.write(gen.ops().read(target.type()));
    }

    // `>> a >> b;` prints each value in order and ends the line.
    private void print(PigParser.PrintStatementContext ctx) {
        for (PigParser.ExpressionContext item : ctx.expression()) {
            gen.ops().print(exprs.expression(item), item);
        }
        gen.ops().printNewline();
    }

    private void ifStatement(PigParser.IfStatementContext ctx) {
        List<Branch> branches = new ArrayList<>();
        Branch current = null;
        for (ParseTree child : ctx.children) {
            if (child instanceof TerminalNode token
                    && (token.getSymbol().getType() == PigParser.SI || token.getSymbol().getType() == PigParser.ALITER)) {
                current = new Branch();
                branches.add(current);
            } else if (child instanceof PigParser.ExpressionContext condition) {
                current.condition = condition;
            } else if (child instanceof PigParser.StatementContext statement) {
                current.body.add(statement);
            }
        }
        branch(branches, 0);
    }

    // Each arm becomes the else part of the previous one.
    private void branch(List<Branch> branches, int index) {
        Branch arm = branches.get(index);
        if (arm.condition == null) {
            statements(arm.body);
            return;
        }
        Runnable otherwise = index + 1 < branches.size() ? () -> branch(branches, index + 1) : null;
        gen.flow().ifElse(() -> exprs.expression(arm.condition), () -> statements(arm.body), otherwise);
    }

    // Of the direct expressions of `per`, the one before the second ';' is the condition and the other the update.
    private void forStatement(PigParser.ForStatementContext ctx) {
        int secondSemi = ctx.SEMI(1).getSymbol().getTokenIndex();
        PigParser.ExpressionContext condition = null;
        PigParser.ExpressionContext update = null;
        for (PigParser.ExpressionContext expression : ctx.expression()) {
            if (expression.getStart().getTokenIndex() < secondSemi) {
                condition = expression;
            } else {
                update = expression;
            }
        }
        PigParser.ExpressionContext test = condition;
        PigParser.ExpressionContext step = update;
        gen.scope().push();
        gen.flow().forLoop(
                ctx.forInit() == null ? null : () -> forInit(ctx.forInit()),
                test == null ? null : () -> exprs.expression(test),
                step == null ? null : () -> exprs.expression(step),
                () -> statements(ctx.statement()));
        gen.scope().pop();
    }

    private void forInit(PigParser.ForInitContext ctx) {
        if (ctx.IDENTIFIER() == null) {
            exprs.expression(ctx.expression());
            return;
        }
        ExprValue value = exprs.expression(ctx.expression());
        GenType type = PigGenTypeMapper.map(ctx.tipoBasico());
        LocalScope.Variable variable = gen.scope().declareLocal(ctx.IDENTIFIER().getText(), type);
        gen.members().variableTarget(new ExprValue(variable.operand(), type)).write(value);
    }
}
