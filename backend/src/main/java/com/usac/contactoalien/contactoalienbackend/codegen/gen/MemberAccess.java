package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.FieldInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ProgramIndex;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.function.Supplier;

/**
Reading and writing the parts of heap values for the three front ends: fields of objects and
structures, array elements, creation of arrays and structures, and the assignable Target built
on each of them. Writes convert integers to decimal when the destination is decimal.
*/
public final class MemberAccess {

    private final QuadBuilder quads;
    private final Operations ops;
    private final ProgramIndex index;

    public MemberAccess(QuadBuilder quads, Operations ops, ProgramIndex index) {
        this.quads = quads;
        this.ops = ops;
        this.index = index;
    }

    public ClassInfo classOf(GenType type, ParserRuleContext at) {
        ClassInfo classInfo = type.kind() == GenType.Kind.OBJECT ? index.classInfo(type.className()) : null;
        if (classInfo == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_CLASS + type.className(), at);
        }
        return classInfo;
    }

    public FieldInfo fieldInfo(ExprValue object, String name, ParserRuleContext at) {
        FieldInfo field = classOf(object.type(), at).field(name);
        if (field == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_MEMBER + name, at);
        }
        return field;
    }

    public ExprValue readElement(ExprValue array, ExprValue position, ParserRuleContext at) {
        requireArray(array, at);
        String result = quads.emitTemp(QuadOp.LOAD_INDEX, array.operand(), position.operand());
        return new ExprValue(result, array.type().element());
    }

    // New array whose length may be a run-time operand: zero-filled, except that text elements start empty.
    public ExprValue newArray(GenType element, String length) {
        return newArray(element, length, true);
    }

    // fillText false leaves text elements null (Zetariano); true starts them as "" (Pig and Y?).
    public ExprValue newArray(GenType element, String length, boolean fillText) {
        ExprValue array = new ExprValue(quads.emitTemp(QuadOp.NEW_ARRAY, length, null), GenType.array(element));
        if (fillText && element.kind() == GenType.Kind.STRING) {
            fillElements(array, length, () -> Operand.stringLiteral(""));
        } else if (fillText && structOf(element) != null) {
            ClassInfo struct = structOf(element);
            fillElements(array, length, () -> newStruct(struct).operand());
        }
        return array;
    }

    // A Y? structure (id 0) as opposed to a Zetariano class; only structures are created by default.
    private ClassInfo structOf(GenType type) {
        ClassInfo info = type.kind() == GenType.Kind.OBJECT ? index.classInfo(type.className()) : null;
        return info != null && info.id() == 0 ? info : null;
    }

    // Stores a fresh value (made inside the loop) in every element of a new array.
    private void fillElements(ExprValue array, String length, Supplier<String> value) {
        String position = quads.temp();
        String start = quads.newLabel();
        String end = quads.newLabel();
        quads.assign(position, "0");
        quads.mark(start);
        quads.jumpIfFalse(quads.emitTemp(QuadOp.LT, position, length), end);
        quads.emit(QuadOp.STORE_INDEX, value.get(), array.operand(), position);
        quads.assign(position, quads.emitTemp(QuadOp.ADD, position, "1"));
        quads.jump(start);
        quads.mark(end);
    }

    // `leaf[l0][l1]...`: an array of l0 references to arrays of l1 elements and so on; the inner arrays are allocated in a loop.
    public ExprValue newNestedArray(GenType leaf, List<String> lengths, int level, boolean fillText) {
        int remaining = lengths.size() - level;
        GenType element = leaf;
        for (int i = 1; i < remaining; i++) {
            element = GenType.array(element);
        }
        String length = lengths.get(level);
        ExprValue array = newArray(element, length, fillText && remaining == 1);
        if (remaining == 1) {
            return array;
        }
        String position = quads.temp();
        String start = quads.newLabel();
        String end = quads.newLabel();
        quads.assign(position, "0");
        quads.mark(start);
        quads.jumpIfFalse(quads.emitTemp(QuadOp.LT, position, length), end);
        quads.emit(QuadOp.STORE_INDEX, newNestedArray(leaf, lengths, level + 1, fillText).operand(), array.operand(), position);
        quads.assign(position, quads.emitTemp(QuadOp.ADD, position, "1"));
        quads.jump(start);
        quads.mark(end);
        return array;
    }

    // Array holding the given values in order; the element type is decimal if any value is.
    public ExprValue arrayOf(List<ExprValue> values) {
        boolean decimal = values.stream().anyMatch(value -> value.type().isFloat());
        GenType element = decimal ? GenType.FLOAT : values.get(0).type();
        ExprValue array = newArray(element, Integer.toString(values.size()));
        for (int i = 0; i < values.size(); i++) {
            ExprValue converted = ops.coerce(values.get(i), element);
            quads.emit(QuadOp.STORE_INDEX, converted.operand(), array.operand(), Integer.toString(i));
        }
        return array;
    }

    // New structure with every field at its default: arrays allocated with their declared length, text empty.
    public ExprValue newStruct(ClassInfo struct) {
        String reference = quads.temp();
        quads.emit(QuadOp.NEW, Integer.toString(struct.sizeInBytes()), Integer.toString(struct.id()), reference);
        ExprValue object = new ExprValue(reference, GenType.object(struct.name()));
        for (FieldInfo field : struct.fields()) {
            if (field.type().kind() == GenType.Kind.ARRAY && field.length() > 0) {
                ExprValue array = newArray(field.type().element(), Integer.toString(field.length()));
                fieldTarget(object, field).write(array);
            } else if (field.type().kind() == GenType.Kind.STRING) {
                fieldTarget(object, field).write(new ExprValue(Operand.stringLiteral(""), GenType.STRING));
            } else if (structOf(field.type()) != null && !field.type().className().equals(struct.name())) {
                fieldTarget(object, field).write(newStruct(structOf(field.type())));
            }
        }
        return object;
    }

    // New structure whose fields take the given values in declaration order; the rest keep their defaults.
    public ExprValue structOf(ClassInfo struct, List<ExprValue> values) {
        ExprValue object = newStruct(struct);
        List<FieldInfo> fields = struct.fields();
        for (int i = 0; i < values.size() && i < fields.size(); i++) {
            fieldTarget(object, fields.get(i)).write(values.get(i));
        }
        return object;
    }

    public Target variableTarget(ExprValue variable) {
        return new Target() {
            @Override
            public GenType type() {
                return variable.type();
            }

            @Override
            public ExprValue read() {
                return variable;
            }

            @Override
            public ExprValue write(ExprValue value) {
                ExprValue converted = ops.coerce(value, variable.type());
                quads.assign(variable.operand(), converted.operand());
                return converted;
            }
        };
    }

    public Target fieldTarget(ExprValue object, FieldInfo field) {
        return new Target() {
            @Override
            public GenType type() {
                return field.type();
            }

            @Override
            public ExprValue read() {
                String result = quads.emitTemp(QuadOp.LOAD_FIELD, object.operand(), Integer.toString(field.offset()));
                return new ExprValue(result, field.type());
            }

            @Override
            public ExprValue write(ExprValue value) {
                ExprValue converted = ops.coerce(value, field.type());
                quads.emit(QuadOp.STORE_FIELD, converted.operand(), object.operand(), Integer.toString(field.offset()));
                return converted;
            }
        };
    }

    public Target elementTarget(ExprValue array, ExprValue position, ParserRuleContext at) {
        requireArray(array, at);
        return new Target() {
            @Override
            public GenType type() {
                return array.type().element();
            }

            @Override
            public ExprValue read() {
                return readElement(array, position, at);
            }

            @Override
            public ExprValue write(ExprValue value) {
                ExprValue converted = ops.coerce(value, array.type().element());
                quads.emit(QuadOp.STORE_INDEX, converted.operand(), array.operand(), position.operand());
                return converted;
            }
        };
    }

    private void requireArray(ExprValue array, ParserRuleContext at) {
        if (array.type().kind() != GenType.Kind.ARRAY) {
            throw GenerationException.unsupported(GenerationMessages.FEATURE_ARRAYS, at);
        }
    }
}
