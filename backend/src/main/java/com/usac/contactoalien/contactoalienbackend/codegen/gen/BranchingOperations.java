package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;

import java.util.function.Supplier;

/**
Operators that skip part of their operands at run time: short-circuit && and || and the
conditional expression. Operations delegates to this class so the operand lowering stays small.
*/
final class BranchingOperations {

    private final QuadBuilder quads;
    private final Operations ops;

    BranchingOperations(QuadBuilder quads, Operations ops) {
        this.quads = quads;
        this.ops = ops;
    }

    // Short-circuit && and ||: the right operand is only evaluated when the left one does not decide.
    ExprValue logical(boolean isAnd, Supplier<ExprValue> left, Supplier<ExprValue> right) {
        String result = quads.temp();
        String end = quads.newLabel();
        quads.assign(result, left.get().operand());
        if (isAnd) {
            quads.jumpIfFalse(result, end);
        } else {
            quads.jumpIfTrue(result, end);
        }
        quads.assign(result, right.get().operand());
        quads.mark(end);
        return new ExprValue(result, GenType.BOOL);
    }

    // condition ? whenTrue : whenFalse
    ExprValue conditional(Supplier<ExprValue> condition, Supplier<ExprValue> whenTrue,
                          Supplier<ExprValue> whenFalse) {
        String result = quads.temp();
        String otherwise = quads.newLabel();
        String end = quads.newLabel();
        String trueDone = quads.newLabel();
        quads.jumpIfFalse(condition.get().operand(), otherwise);
        ExprValue first = whenTrue.get();
        quads.assign(result, first.operand());
        quads.jump(trueDone);
        quads.mark(otherwise);
        ExprValue second = whenFalse.get();
        // One decimal branch and one integer branch: the result is decimal, so the integer side is widened.
        boolean widenFirst = second.type().isFloat() && first.type().isNumeric() && !first.type().isFloat();
        quads.assign(result, ops.coerce(second, first.type().isFloat() ? GenType.FLOAT : null).operand());
        quads.jump(end);
        quads.mark(trueDone);
        if (widenFirst) {
            quads.assign(result, quads.emitTemp(QuadOp.I2F, result, null));
        }
        quads.mark(end);
        GenType type = first.type().kind() == GenType.Kind.NULL ? second.type()
                : widenFirst ? GenType.FLOAT : first.type();
        return new ExprValue(result, type);
    }
}
