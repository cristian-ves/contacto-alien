package com.usac.contactoalien.contactoalienbackend.codegen.gen.y;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ControlFlow;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LocalScope;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.OperatorFold;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

// Translates the statements of a Y? function body; the block layout itself comes from ControlFlow.
final class YStatementGen {

    private final GenContext gen;
    private final YExpressionGen exprs;
    private final GenType returnType;

    YStatementGen(GenContext gen, YExpressionGen exprs, GenType returnType) {
        this.gen = gen;
        this.exprs = exprs;
        this.returnType = returnType;
    }

    void block(List<YParser.StatementContext> statements) {
        gen.scope().push();
        for (YParser.StatementContext statement : statements) {
            statement(statement);
        }
        gen.scope().pop();
    }

    private void statement(YParser.StatementContext ctx) {
        switch (ctx) {
            case YParser.VariableDeclarationStatementContext s -> declaration(s.variableDeclaration());
            case YParser.IfStmtContext s -> ifStatement(s.ifStatement());
            case YParser.ForStmtContext s -> forStatement(s.forStatement());
            case YParser.WhileStmtContext s -> gen.flow().whileLoop(
                    () -> exprs.expression(s.whileStatement().expression()),
                    () -> block(s.whileStatement().statement()));
            case YParser.DoWhileStmtContext s -> gen.flow().doWhile(
                    () -> block(s.doWhileStatement().statement()),
                    () -> exprs.expression(s.doWhileStatement().expression()));
            case YParser.ReturnStatementContext s -> returnStatement(s);
            case YParser.BreakStatementContext s -> gen.flow().breakLoop(s);
            case YParser.ContinueStatementContext s -> gen.flow().continueLoop(s);
            case YParser.ExpressionStatementContext s -> exprs.expression(s.expression());
            case YParser.SwitchStmtContext s -> switchStatement(s.switchStatement());
            case YParser.NestedStructStatementContext s -> {
                // registered with the rest of the structures before any function is translated
            }
            default -> throw GenerationException.unsupported(GenerationMessages.FEATURE_STRUCTS, ctx);
        }
    }

    // `tipo x = v`, `tipo a[n]` (array of n elements, or filled by a {..} list) or a structure variable.
    private void declaration(YParser.VariableDeclarationContext ctx) {
        GenType element = YTypeMapper.map(ctx.type());
        int dimensions = ctx.LBRACK().size();
        GenType type = element;
        for (int i = 0; i < dimensions; i++) {
            type = GenType.array(type);
        }
        YParser.PrimaryContext list = ctx.expression() == null ? null
                : OperatorFold.descend(ctx.expression(), YParser.PrimaryContext.class);
        boolean structList = list != null && list.LBRACE() != null && element.kind() == GenType.Kind.OBJECT
                && dimensions == 0;
        ExprValue value = ctx.expression() == null ? null : structList
                ? gen.members().structOf(gen.members().classOf(element, ctx), list.expression().stream().map(exprs::expression).toList())
                : exprs.typed(ctx.expression(), type);
        if (value == null && dimensions > 0) {
            value = gen.members().newNestedArray(element, ctx.INT_LITERAL().stream().map(TerminalNode::getText).toList(), 0, true);
        } else if (value == null && element.kind() == GenType.Kind.OBJECT) {
            value = gen.members().newStruct(gen.members().classOf(element, ctx));
        } else if (value == null) {
            value = new ExprValue(type.defaultOperand(), type);
        }
        LocalScope.Variable variable = gen.scope().declareLocal(ctx.IDENTIFIER().getText(), type);
        gen.members().variableTarget(new ExprValue(variable.operand(), type)).write(value);
    }

    private void returnStatement(YParser.ReturnStatementContext ctx) {
        String value = null;
        if (ctx.expression() != null) {
            value = gen.ops().coerce(exprs.typed(ctx.expression(), returnType), returnType).operand();
        }
        gen.quads().emit(QuadOp.RETURN, value, null, null);
    }

    // elegir(x): caso v ... siempre ...; romper leaves the switch and the arms fall through otherwise.
    private void switchStatement(YParser.SwitchStatementContext ctx) {
        List<ControlFlow.SwitchArm> arms = new ArrayList<>();
        for (YParser.CaseClauseContext clause : ctx.caseClause()) {
            arms.add(new ControlFlow.SwitchArm(() -> exprs.expression(clause.expression()), () -> block(clause.statement())));
        }
        if (ctx.defaultClause() != null) {
            arms.add(new ControlFlow.SwitchArm(null, () -> block(ctx.defaultClause().statement())));
        }
        gen.flow().switchOn(exprs.expression(ctx.expression()), arms, ctx);
    }

    // si / sino (...) / contrario: every sino and the contrario nest as the else part of the previous arm.
    private void ifStatement(YParser.IfStatementContext ctx) {
        arm(ctx.expression(), ctx.statement(), ctx.elseIfClause(), 0, ctx.elseClause());
    }

    private void arm(YParser.ExpressionContext condition, List<YParser.StatementContext> body,
                     List<YParser.ElseIfClauseContext> elseIfs, int next, YParser.ElseClauseContext last) {
        Runnable otherwise = null;
        if (next < elseIfs.size()) {
            YParser.ElseIfClauseContext clause = elseIfs.get(next);
            otherwise = () -> arm(clause.expression(), clause.statement(), elseIfs, next + 1, last);
        } else if (last != null) {
            otherwise = () -> block(last.statement());
        }
        gen.flow().ifElse(() -> exprs.expression(condition), () -> block(body), otherwise);
    }

    private void forStatement(YParser.ForStatementContext ctx) {
        YParser.ForInitContext init = ctx.forInit();
        gen.scope().push();
        gen.flow().forLoop(
                init == null ? null : () -> forInit(init),
                ctx.expression() == null ? null : () -> exprs.expression(ctx.expression()),
                ctx.forUpdate() == null ? null : () -> exprs.expression(ctx.forUpdate().expression()),
                () -> block(ctx.statement()));
        gen.scope().pop();
    }

    private void forInit(YParser.ForInitContext ctx) {
        if (ctx.IDENTIFIER() == null) {
            exprs.expression(ctx.expression());
            return;
        }
        GenType type = YTypeMapper.map(ctx.type());
        ExprValue value = exprs.typed(ctx.expression(), type);
        LocalScope.Variable variable = gen.scope().declareLocal(ctx.IDENTIFIER().getText(), type);
        gen.members().variableTarget(new ExprValue(variable.operand(), type)).write(value);
    }
}
