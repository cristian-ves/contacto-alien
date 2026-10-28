package com.usac.contactoalien.contactoalienbackend.codegen.gen.pig;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.Target;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.FunctionInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.MethodInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

/**
Postfix chains of Pig Latin expressions: calls of imported Y? functions, member access and
method calls on Zetariano objects, object creation, array elements, ++/-- and assignment targets.
*/
final class PigPostfixGen {

    private final GenContext gen;
    private final PigExpressionGen exprs;

    PigPostfixGen(GenContext gen, PigExpressionGen exprs) {
        this.gen = gen;
        this.exprs = exprs;
    }

    // Used by `esto o : novus Clase(...)` declarations as well as by `novus` expressions.
    ExprValue instantiate(String className, PigParser.ArgumentListContext args, ParserRuleContext at) {
        ClassInfo classInfo = gen.index().classInfo(className);
        if (classInfo == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_CLASS + className, at);
        }
        List<ExprValue> arguments = arguments(args);
        MethodInfo constructor = classInfo.findConstructor(types(arguments));
        return gen.calls().newObject(classInfo, constructor, arguments);
    }

    /**
    Assigns to a variable, a field of an object or an element of an array. Returns the stored value
    so that assignments can be used as expressions.
    */
    ExprValue assignTo(PigParser.PostfixExpressionContext target, ExprValue value) {
        return place(target).write(value);
    }

    Target place(PigParser.PostfixExpressionContext target) {
        return target(target, target.postfixOp().size());
    }

    // The assignable place named by the primary expression followed by the first opCount operators.
    private Target target(PigParser.PostfixExpressionContext ctx, int opCount) {
        PigParser.PrimaryExpressionContext primary = ctx.primaryExpression();
        if (opCount == 0 && primary.IDENTIFIER() != null && primary.NOVUS() == null) {
            return gen.members().variableTarget(gen.variable(primary.IDENTIFIER().getText(), ctx));
        }
        PigParser.PostfixOpContext last = opCount == 0 ? null : ctx.postfixOp().get(opCount - 1);
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

    /**
    Evaluates the primary expression followed by the first opCount postfix operators: a call
    of an imported function, member accesses, array elements, method calls and ++/-- on a variable.
    */
    ExprValue evaluate(PigParser.PostfixExpressionContext ctx, int opCount) {
        List<PigParser.PostfixOpContext> ops = ctx.postfixOp();
        PigParser.PrimaryExpressionContext primary = ctx.primaryExpression();
        int i = 0;
        ExprValue current;
        if (isFunctionCall(primary, ops, opCount)) {
            current = callFunction(primary, ops.get(0));
            i = 1;
        } else {
            current = exprs.primary(primary);
        }
        for (; i < opCount; i++) {
            PigParser.PostfixOpContext op = ops.get(i);
            if (op.DOT() != null) {
                String member = op.IDENTIFIER().getText();
                boolean isCall = i + 1 < opCount && ops.get(i + 1).LPAREN() != null;
                current = isCall ? callMethod(current, member, ops.get(++i)) : readField(current, member, ctx);
            } else if (op.LBRACK() != null) {
                current = gen.members().readElement(current, exprs.expression(op.expression()), ctx);
            } else if (op.INC() != null || op.DEC() != null) {
                current = step(target(ctx, i), op.INC() != null, ctx);
            } else {
                throw GenerationException.unsupported(GenerationMessages.FEATURE_EXPRESSION, ctx);
            }
        }
        return current;
    }

    private boolean isFunctionCall(PigParser.PrimaryExpressionContext primary,
                                   List<PigParser.PostfixOpContext> ops, int opCount) {
        return primary.IDENTIFIER() != null && primary.NOVUS() == null && opCount > 0
                && ops.get(0).LPAREN() != null && gen.scope().lookup(primary.IDENTIFIER().getText()) == null;
    }

    private ExprValue callFunction(PigParser.PrimaryExpressionContext primary, PigParser.PostfixOpContext call) {
        String name = primary.IDENTIFIER().getText();
        FunctionInfo function = gen.index().function(name);
        if (function == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_FUNCTION + name, primary);
        }
        return gen.calls().callFunction(function, arguments(call.argumentList()));
    }

    private ExprValue callMethod(ExprValue receiver, String name, PigParser.PostfixOpContext call) {
        ClassInfo classInfo = classOf(receiver, call);
        List<ExprValue> arguments = arguments(call.argumentList());
        MethodInfo method = classInfo.findMethod(name, types(arguments));
        if (method == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_MEMBER + name, call);
        }
        return gen.calls().callMethod(receiver, method, arguments, true);
    }

    private ExprValue readField(ExprValue object, String name, ParserRuleContext at) {
        return gen.members().fieldTarget(object, gen.members().fieldInfo(object, name, at)).read();
    }

    private ClassInfo classOf(ExprValue object, ParserRuleContext at) {
        return gen.members().classOf(object.type(), at);
    }

    // i++ / i-- on a variable, field or element; the expression's value is the value before the change.
    private ExprValue step(Target place, boolean increment, ParserRuleContext at) {
        ExprValue current = place.read();
        String before = gen.quads().emitTemp(QuadOp.ASSIGN, current.operand(), null);
        place.write(gen.ops().binary(increment ? "+" : "-", current, new ExprValue("1", GenType.INT), at));
        return new ExprValue(before, current.type());
    }

    List<ExprValue> arguments(PigParser.ArgumentListContext list) {
        List<ExprValue> values = new ArrayList<>();
        if (list != null) {
            for (PigParser.ExpressionContext argument : list.expression()) {
                values.add(exprs.expression(argument));
            }
        }
        return values;
    }

    private List<GenType> types(List<ExprValue> values) {
        return values.stream().map(ExprValue::type).toList();
    }
}
