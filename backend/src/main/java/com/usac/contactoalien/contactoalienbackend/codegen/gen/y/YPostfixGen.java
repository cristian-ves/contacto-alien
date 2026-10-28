package com.usac.contactoalien.contactoalienbackend.codegen.gen.y;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.Target;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.FunctionInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
Postfix chains of Y? expressions: calls to functions, fields of structures, array elements and
++/--. The same chain also resolves to an assignable Target, used by assignments and by
increment and decrement on variables, fields and elements.
*/
final class YPostfixGen {

    private final GenContext gen;
    private final YExpressionGen exprs;

    YPostfixGen(GenContext gen, YExpressionGen exprs) {
        this.gen = gen;
        this.exprs = exprs;
    }

    ExprValue evaluate(YParser.PostfixContext ctx) {
        return evaluate(ctx, ctx.postfixOp().size());
    }

    // The primary expression followed by the first opCount operators.
    private ExprValue evaluate(YParser.PostfixContext ctx, int opCount) {
        YParser.PrimaryContext primary = ctx.primary();
        List<YParser.PostfixOpContext> ops = ctx.postfixOp();
        int i = 0;
        ExprValue current;
        if (primary.IDENTIFIER() != null && opCount > 0 && ops.get(0).LPAREN() != null) {
            current = call(primary, ops.get(0));
            i = 1;
        } else {
            current = exprs.visit(primary);
        }
        for (; i < opCount; i++) {
            YParser.PostfixOpContext op = ops.get(i);
            if (op.DOT() != null) {
                current = gen.members().fieldTarget(current, gen.members().fieldInfo(current, op.IDENTIFIER().getText(), ctx)).read();
            } else if (op.LBRACK() != null) {
                current = gen.members().readElement(current, exprs.expression(op.expression()), ctx);
            } else if (op.INC() != null || op.DEC() != null) {
                current = step(target(ctx, i), op.INC() != null, false, ctx);
            } else {
                throw GenerationException.unsupported(GenerationMessages.FEATURE_EXPRESSION, ctx);
            }
        }
        return current;
    }

    // The assignable place named by the primary expression followed by the first opCount operators.
    Target target(YParser.PostfixContext ctx, int opCount) {
        YParser.PrimaryContext primary = ctx.primary();
        if (opCount == 0 && primary.IDENTIFIER() != null) {
            return gen.members().variableTarget(gen.variable(primary.IDENTIFIER().getText(), ctx));
        }
        YParser.PostfixOpContext last = opCount == 0 ? null : ctx.postfixOp().get(opCount - 1);
        if (last != null && last.DOT() != null) {
            ExprValue object = evaluate(ctx, opCount - 1);
            return gen.members().fieldTarget(object, gen.members().fieldInfo(object, last.IDENTIFIER().getText(), ctx));
        }
        if (last != null && last.LBRACK() != null) {
            ExprValue array = evaluate(ctx, opCount - 1);
            return gen.members().elementTarget(array, exprs.expression(last.expression()), ctx);
        }
        throw GenerationException.unsupported(GenerationMessages.FEATURE_ASSIGN_TARGET, ctx);
    }

    // Adds or subtracts one; the value is the one before the change for x++ and after it for ++x.
    ExprValue step(Target place, boolean increment, boolean prefix, ParserRuleContext at) {
        ExprValue current = place.read();
        String before = gen.quads().emitTemp(QuadOp.ASSIGN, current.operand(), null);
        ExprValue updated = gen.ops().binary(increment ? "+" : "-", current, new ExprValue("1", GenType.INT), at);
        ExprValue stored = place.write(updated);
        return prefix ? stored : new ExprValue(before, current.type());
    }

    private ExprValue call(YParser.PrimaryContext primary, YParser.PostfixOpContext call) {
        String name = primary.IDENTIFIER().getText();
        FunctionInfo function = gen.index().function(name);
        if (function == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_FUNCTION + name, primary);
        }
        return gen.calls().callFunction(function, exprs.arguments(call.argumentList(), function.params()));
    }
}
