package com.usac.contactoalien.contactoalienbackend.codegen.gen.z;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;

import java.util.List;

/**
Array creation for Zetariano. `new T[a][b]` is an array of `a` references to arrays of `b`
elements, so the inner arrays are allocated in a loop; elements start at zero (false, 0.0,
null). A braced initializer allocates the array and stores each entry, recursing for nested
braces. Every dimension is evaluated before any allocation, left to right.
*/
final class ZArrayGen {

    private final GenContext gen;
    private final ZExpressionGen exprs;

    ZArrayGen(GenContext gen, ZExpressionGen exprs) {
        this.gen = gen;
        this.exprs = exprs;
    }

    ExprValue create(ZetarianoParser.ArrayCreationContext ctx) {
        List<ExprValue> sizes = ctx.expression().stream().map(exprs::expression).toList();
        return build(ZTypeMapper.leaf(ctx), sizes, 0);
    }

    // Value for a declaration initializer: a plain expression converted to the declared type, or a braced list.
    ExprValue initializer(ZetarianoParser.VariableInitializerContext ctx, GenType declared) {
        if (ctx.expression() != null) {
            return gen.ops().coerce(exprs.expression(ctx.expression()), declared);
        }
        GenType element = declared.element();
        List<ZetarianoParser.VariableInitializerContext> entries = ctx.arrayInitializer().variableInitializer();
        ExprValue array = gen.members().newArray(element, Integer.toString(entries.size()), false);
        for (int i = 0; i < entries.size(); i++) {
            ExprValue value = initializer(entries.get(i), element);
            gen.quads().emit(QuadOp.STORE_INDEX, value.operand(), array.operand(), Integer.toString(i));
        }
        return array;
    }

    private ExprValue build(GenType leaf, List<ExprValue> sizes, int level) {
        return gen.members().newNestedArray(leaf, sizes.stream().map(ExprValue::operand).toList(), level, false);
    }
}
