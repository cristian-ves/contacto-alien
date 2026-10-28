package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
Templates for the structured statements shared by the three languages. Each front end
passes the pieces (condition, body...) as callbacks, so label creation and jump layout
exist in one place. Loops keep a stack of their exit and continue labels for break/continue;
a switch pushes its end label as the break target and keeps the continue label of the loop around it.
*/
public final class ControlFlow {

    private static final String OUTSIDE_LOOP = "break/continue outside of a loop";

    private record Loop(String breakLabel, String continueLabel) {
    }

    // One arm of a switch; value is null for the default arm.
    public record SwitchArm(Supplier<ExprValue> value, Runnable body) {
    }

    private final QuadBuilder quads;
    private final Operations ops;
    private final Deque<Loop> loops = new ArrayDeque<>();

    public ControlFlow(QuadBuilder quads, Operations ops) {
        this.quads = quads;
        this.ops = ops;
    }

    // otherwise may be null.
    public void ifElse(Supplier<ExprValue> condition, Runnable thenBranch, Runnable otherwise) {
        String elseLabel = quads.newLabel();
        quads.jumpIfFalse(condition.get().operand(), elseLabel);
        thenBranch.run();
        if (otherwise == null) {
            quads.mark(elseLabel);
            return;
        }
        String end = quads.newLabel();
        quads.jump(end);
        quads.mark(elseLabel);
        otherwise.run();
        quads.mark(end);
    }

    public void whileLoop(Supplier<ExprValue> condition, Runnable body) {
        String start = quads.newLabel();
        String end = quads.newLabel();
        quads.mark(start);
        quads.jumpIfFalse(condition.get().operand(), end);
        loops.push(new Loop(end, start));
        body.run();
        loops.pop();
        quads.jump(start);
        quads.mark(end);
    }

    public void doWhile(Runnable body, Supplier<ExprValue> condition) {
        String start = quads.newLabel();
        String test = quads.newLabel();
        String end = quads.newLabel();
        quads.mark(start);
        loops.push(new Loop(end, test));
        body.run();
        loops.pop();
        quads.mark(test);
        quads.jumpIfTrue(condition.get().operand(), start);
        quads.mark(end);
    }

    // init, condition and update may be null; continue jumps to the update step.
    public void forLoop(Runnable init, Supplier<ExprValue> condition, Runnable update, Runnable body) {
        String start = quads.newLabel();
        String step = quads.newLabel();
        String end = quads.newLabel();
        if (init != null) {
            init.run();
        }
        quads.mark(start);
        if (condition != null) {
            quads.jumpIfFalse(condition.get().operand(), end);
        }
        loops.push(new Loop(end, step));
        body.run();
        loops.pop();
        quads.mark(step);
        if (update != null) {
            update.run();
        }
        quads.jump(start);
        quads.mark(end);
    }

    /**
    The subject is compared with each case value in order using the language's equality; the
    first match jumps into its body and the bodies fall through to the next one until a break,
    as in C. Without a match the default arm runs, or nothing if there is none.
    */
    public void switchOn(ExprValue subject, List<SwitchArm> arms, ParserRuleContext at) {
        String end = quads.newLabel();
        List<String> labels = new ArrayList<>();
        String defaultLabel = end;
        for (SwitchArm arm : arms) {
            String label = quads.newLabel();
            labels.add(label);
            if (arm.value() == null) {
                defaultLabel = label;
            } else {
                ExprValue test = ops.binary("==", subject, arm.value().get(), at);
                quads.jumpIfTrue(test.operand(), label);
            }
        }
        quads.jump(defaultLabel);
        String enclosingContinue = loops.isEmpty() ? null : loops.peek().continueLabel();
        loops.push(new Loop(end, enclosingContinue));
        for (int i = 0; i < arms.size(); i++) {
            quads.mark(labels.get(i));
            arms.get(i).body().run();
        }
        loops.pop();
        quads.mark(end);
    }

    public void breakLoop(ParserRuleContext at) {
        if (loops.isEmpty()) {
            throw new GenerationException(OUTSIDE_LOOP, at);
        }
        quads.jump(loops.peek().breakLabel());
    }

    public void continueLoop(ParserRuleContext at) {
        Loop target = loops.peek();
        if (target == null || target.continueLabel() == null) {
            throw new GenerationException(OUTSIDE_LOOP, at);
        }
        quads.jump(target.continueLabel());
    }
}
