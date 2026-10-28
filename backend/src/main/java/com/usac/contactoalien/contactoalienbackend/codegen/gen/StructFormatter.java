package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.FieldInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ProgramIndex;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayDeque;
import java.util.Deque;

/**
Text of a Y? structure: Name{field=value, ...} with nested structures expanded in place and
array fields as [a, b, c]. The statements are expanded at compile time by walking the declared
fields, so printing needs no runtime type information. Classes of Zetariano (id other than 0)
are not printable.
*/
public final class StructFormatter implements ObjectFormatter {

    private static final String OPEN = "{";
    private static final String CLOSE = "}";
    private static final String SEPARATOR = ", ";
    private static final String ASSIGN = "=";
    private static final String ARRAY_OPEN = "[";
    private static final String ARRAY_CLOSE = "]";
    private static final String CYCLE = "...";

    private final QuadBuilder quads;
    private final Operations ops;
    private final ProgramIndex index;
    private final MemberAccess members;
    // Structures being expanded right now, so a type that contains itself stops at the second level.
    private final Deque<String> expanding = new ArrayDeque<>();

    public StructFormatter(QuadBuilder quads, Operations ops, ProgramIndex index, MemberAccess members) {
        this.quads = quads;
        this.ops = ops;
        this.index = index;
        this.members = members;
    }

    @Override
    public ExprValue format(ExprValue value, ParserRuleContext at) {
        ClassInfo struct = value.type().kind() == GenType.Kind.OBJECT ? index.classInfo(value.type().className()) : null;
        if (struct == null || struct.id() != 0) {
            throw GenerationException.unsupported(GenerationMessages.FEATURE_OBJECT_PRINT, at);
        }
        String text = quads.temp();
        quads.assign(text, Operand.stringLiteral(struct.name() + OPEN));
        if (expanding.contains(struct.name())) {
            append(text, Operand.stringLiteral(CYCLE + CLOSE));
            return new ExprValue(text, GenType.STRING);
        }
        expanding.push(struct.name());
        boolean first = true;
        for (FieldInfo field : struct.fields()) {
            append(text, Operand.stringLiteral((first ? "" : SEPARATOR) + field.name() + ASSIGN));
            first = false;
            ExprValue fieldValue = members.fieldTarget(value, field).read();
            append(text, valueText(fieldValue, at));
        }
        expanding.pop();
        append(text, Operand.stringLiteral(CLOSE));
        return new ExprValue(text, GenType.STRING);
    }

    private String valueText(ExprValue value, ParserRuleContext at) {
        if (value.type().kind() == GenType.Kind.ARRAY) {
            return arrayText(value, at);
        }
        return ops.toStringValue(value, at).operand();
    }

    // [e0, e1, ...] built with a run-time loop, since the length is only known when the program runs.
    private String arrayText(ExprValue array, ParserRuleContext at) {
        String text = quads.temp();
        String position = quads.temp();
        String length = quads.emitTemp(QuadOp.LOAD_FIELD, array.operand(), "0");
        String start = quads.newLabel();
        String end = quads.newLabel();
        String firstElement = quads.newLabel();
        quads.assign(text, Operand.stringLiteral(ARRAY_OPEN));
        quads.assign(position, "0");
        quads.mark(start);
        quads.jumpIfFalse(quads.emitTemp(QuadOp.LT, position, length), end);
        quads.jumpIfFalse(quads.emitTemp(QuadOp.GT, position, "0"), firstElement);
        append(text, Operand.stringLiteral(SEPARATOR));
        quads.mark(firstElement);
        ExprValue element = members.readElement(array, new ExprValue(position, GenType.INT), at);
        append(text, valueText(element, at));
        quads.assign(position, quads.emitTemp(QuadOp.ADD, position, "1"));
        quads.jump(start);
        quads.mark(end);
        append(text, Operand.stringLiteral(ARRAY_CLOSE));
        return text;
    }

    private void append(String text, String piece) {
        quads.assign(text, quads.emitTemp(QuadOp.STR_CONCAT, text, piece));
    }
}
