package com.usac.contactoalien.contactoalienbackend.codegen.gen.z;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.Target;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.FieldInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.MethodInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
Object and array parts of Zetariano expressions: `new`, method calls, field and element
reads, assignable targets (variables, fields, array elements) and ++/--. A postfix chain is
evaluated left to right; a name resolves to a local first and to a field of the current object second.
*/
final class ZMemberGen {

    private static final String ARRAY_LENGTH = "length";

    private final GenContext gen;
    private final ClassInfo current;
    private final ZExpressionGen exprs;

    ZMemberGen(GenContext gen, ClassInfo current, ZExpressionGen exprs) {
        this.gen = gen;
        this.current = current;
        this.exprs = exprs;
    }

    ExprValue instantiate(ZetarianoParser.PrimaryExpressionContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        ClassInfo classInfo = gen.index().classInfo(name);
        if (classInfo == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_CLASS + name, ctx);
        }
        List<ExprValue> arguments = exprs.arguments(ctx.argumentList());
        return gen.calls().newObject(classInfo, classInfo.findConstructor(types(arguments)), arguments);
    }

    // A primary followed by member accesses, calls, indexes and an optional trailing ++/--.
    ExprValue postfix(ZetarianoParser.PostfixExpressionContext ctx) {
        List<ZetarianoParser.PostfixOpContext> ops = ctx.postfixOp();
        int count = ops.size();
        if (count > 0 && (ops.get(count - 1).PLUS_PLUS() != null || ops.get(count - 1).MINUS_MINUS() != null)) {
            return step(target(ctx, count - 1), ops.get(count - 1).PLUS_PLUS() != null, true, ctx);
        }
        return walk(ctx, count);
    }

    // Lvalue of the first opCount operators: a name, a field or an element.
    Target target(ZetarianoParser.PostfixExpressionContext ctx, int opCount) {
        ZetarianoParser.PrimaryExpressionContext primary = ctx.primaryExpression();
        if (opCount == 0 && primary.IDENTIFIER() != null && primary.NEW() == null) {
            return targetOf(primary.IDENTIFIER().getText(), primary);
        }
        if (opCount > 0) {
            ZetarianoParser.PostfixOpContext last = ctx.postfixOp().get(opCount - 1);
            if (last.DOT() != null) {
                ExprValue object = walk(ctx, opCount - 1);
                return gen.members().fieldTarget(object, gen.members().fieldInfo(object, last.IDENTIFIER().getText(), ctx));
            }
            if (last.LBRACKET() != null) {
                ExprValue array = walk(ctx, opCount - 1);
                return gen.members().elementTarget(array, exprs.expression(last.expression()), ctx);
            }
        }
        throw GenerationException.unsupported(GenerationMessages.FEATURE_ASSIGN_TARGET, ctx);
    }

    Target targetOf(String name, ParserRuleContext at) {
        if (gen.scope().lookup(name) != null) {
            return gen.members().variableTarget(gen.variable(name, at));
        }
        FieldInfo field = current.field(name);
        if (field == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_VARIABLE + name, at);
        }
        return gen.members().fieldTarget(self(at), field);
    }

    /**
    ++ and --: the postfix form yields the old value, the prefix form the new one.
    */
    ExprValue step(Target target, boolean increment, boolean postfix, ParserRuleContext at) {
        ExprValue old = target.read();
        ExprValue before = postfix ? new ExprValue(gen.quads().emitTemp(QuadOp.ASSIGN, old.operand(), null), old.type()) : null;
        ExprValue updated = target.write(gen.ops().binary(increment ? "+" : "-", old, new ExprValue("1", GenType.INT), at));
        return postfix ? before : updated;
    }

    ExprValue self(ParserRuleContext at) {
        return gen.variable(ZExpressionGen.THIS, at);
    }

    // Evaluates the primary and the first count operators; a DOT name followed by a call is one method call.
    private ExprValue walk(ZetarianoParser.PostfixExpressionContext ctx, int count) {
        List<ZetarianoParser.PostfixOpContext> ops = ctx.postfixOp();
        ZetarianoParser.PrimaryExpressionContext primary = ctx.primaryExpression();
        int i = 0;
        ExprValue value;
        if (primary.SUPER() != null) {
            if (count == 0) {
                return superReference(primary);
            }
            value = superMember(ops, primary);
            i = ops.get(0).LPAREN() != null ? 1 : isCallAt(ops, 1) ? 2 : 1;
        } else if (primary.IDENTIFIER() != null && primary.NEW() == null && count > 0 && ops.get(0).LPAREN() != null) {
            value = invoke(self(primary), primary.IDENTIFIER().getText(), ops.get(0), primary);
            i = 1;
        } else {
            value = exprs.visit(primary);
        }
        for (; i < count; i++) {
            ZetarianoParser.PostfixOpContext op = ops.get(i);
            if (op.LBRACKET() != null) {
                value = gen.members().readElement(value, exprs.expression(op.expression()), ctx);
            } else if (op.DOT() != null) {
                String member = op.IDENTIFIER().getText();
                boolean isCall = i + 1 < count && ops.get(i + 1).LPAREN() != null;
                value = isCall ? invoke(value, member, ops.get(++i), ctx) : read(value, member, ctx);
            } else {
                throw GenerationException.unsupported(GenerationMessages.FEATURE_EXPRESSION, ctx);
            }
        }
        return value;
    }

    private static boolean isCallAt(List<ZetarianoParser.PostfixOpContext> ops, int position) {
        return position < ops.size() && ops.get(position).LPAREN() != null;
    }

    // `super` as an object: this, seen as an instance of the superclass.
    ExprValue superReference(ParserRuleContext at) {
        return new ExprValue(self(at).operand(), GenType.object(superclass(at).name()));
    }

    private ClassInfo superclass(ParserRuleContext at) {
        if (current.parent() == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_CLASS + "super", at);
        }
        return current.parent();
    }

    /**
    The first operator after `super`: `super(args)` runs the constructor of the superclass on this object,
    `super.m(args)` calls the method of the superclass without dynamic dispatch, `super.f` reads a field.
    */
    private ExprValue superMember(List<ZetarianoParser.PostfixOpContext> ops, ParserRuleContext at) {
        ClassInfo parent = superclass(at);
        ExprValue self = self(at);
        ZetarianoParser.PostfixOpContext first = ops.get(0);
        if (first.LPAREN() != null) {
            List<ExprValue> arguments = exprs.arguments(first.argumentList());
            gen.calls().construct(parent, parent.findConstructor(types(arguments)), self, arguments);
            return new ExprValue(null, GenType.VOID);
        }
        String member = first.IDENTIFIER().getText();
        if (isCallAt(ops, 1)) {
            List<ExprValue> arguments = exprs.arguments(ops.get(1).argumentList());
            MethodInfo method = parent.findMethod(member, types(arguments));
            if (method == null) {
                throw new GenerationException(GenerationMessages.UNKNOWN_MEMBER + member, at);
            }
            return gen.calls().callDirect(self, method, arguments);
        }
        return read(superReference(at), member, at);
    }

    private ExprValue invoke(ExprValue receiver, String name, ZetarianoParser.PostfixOpContext call,
                             ParserRuleContext at) {
        List<ExprValue> arguments = exprs.arguments(call.argumentList());
        MethodInfo method = gen.members().classOf(receiver.type(), at).findMethod(name, types(arguments));
        if (method == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_MEMBER + name, at);
        }
        boolean onThis = receiver.operand() != null && receiver.operand().equals(self(at).operand());
        return gen.calls().callMethod(receiver, method, arguments, !onThis);
    }

    // The length of an array is the first word of its record.
    private ExprValue read(ExprValue object, String name, ParserRuleContext at) {
        if (object.type().kind() == GenType.Kind.ARRAY && name.equals(ARRAY_LENGTH)) {
            return new ExprValue(gen.quads().emitTemp(QuadOp.LOAD_FIELD, object.operand(), "0"), GenType.INT);
        }
        return gen.members().fieldTarget(object, gen.members().fieldInfo(object, name, at)).read();
    }

    private List<GenType> types(List<ExprValue> values) {
        return values.stream().map(ExprValue::type).toList();
    }
}
