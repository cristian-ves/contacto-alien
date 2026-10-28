package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.FieldInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.FunctionInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.MethodInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ProgramIndex;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;

import java.util.List;

/**
Quadruples for calls and heap objects, shared by the three front ends. Arguments are already
evaluated into operands; a call emits one PARAM per argument (receiver first for methods)
followed by CALL. Methods and constructors receive the object as their first argument.
An integer passed where the callee declares a decimal parameter is converted first.
*/
public final class CallEmitter {

    private final QuadBuilder quads;
    private final Operations ops;
    private final ProgramIndex index;

    public CallEmitter(QuadBuilder quads, Operations ops, ProgramIndex index) {
        this.quads = quads;
        this.ops = ops;
        this.index = index;
    }

    public ExprValue callFunction(FunctionInfo function, List<ExprValue> arguments) {
        return invoke(function.label(), false, null, arguments, function.params(), function.returnType());
    }

    /**
    Calls a method on an object. When some subclass overrides the method, the routine to run depends on
    the class of the object at run time, so its address is read from the class's method table (which also
    fails on a null receiver); otherwise the call goes straight to the method's label, so the receiver is
    checked first. The check is skipped when the receiver is `this`, which is never null.
    */
    public ExprValue callMethod(ExprValue receiver, MethodInfo method, List<ExprValue> arguments, boolean checkReceiver) {
        ClassInfo staticClass = receiver.type().kind() == GenType.Kind.OBJECT ? index.classInfo(receiver.type().className()) : null;
        int slot = staticClass == null ? -1 : staticClass.slotOf(method.key());
        if (slot < 0 || !index.isOverridden(method)) {
            if (checkReceiver) {
                quads.emit(QuadOp.NULL_CHECK, receiver.operand(), null, null);
            }
            return callDirect(receiver, method, arguments);
        }
        String address = quads.emitTemp(QuadOp.LOAD_METHOD, receiver.operand(), Integer.toString(slot));
        return invoke(address, true, receiver, arguments, method.params(), method.returnType());
    }

    // A call fixed at compile time: `super.m(...)`, private methods and methods nobody overrides.
    public ExprValue callDirect(ExprValue receiver, MethodInfo method, List<ExprValue> arguments) {
        return invoke(method.label(), false, receiver, arguments, method.params(), method.returnType());
    }

    /**
    Runs the constructor chosen for an object that is already allocated. A class with no constructor of its
    own has the implicit one, which does nothing but call super(); that call goes to the nearest ancestor
    that declares constructors, using its constructor without parameters.
    */
    public void construct(ClassInfo classInfo, MethodInfo constructor, ExprValue object, List<ExprValue> arguments) {
        if (constructor != null) {
            invoke(constructor.label(), false, object, arguments, constructor.params(), GenType.VOID);
        } else {
            callImplicitSuper(classInfo, object);
        }
    }

    // The call a constructor makes to super() when it does not write one.
    public void callImplicitSuper(ClassInfo classInfo, ExprValue object) {
        for (ClassInfo ancestor = classInfo.parent(); ancestor != null; ancestor = ancestor.parent()) {
            MethodInfo noArguments = ancestor.findConstructor(List.of());
            if (noArguments != null && noArguments.params().isEmpty()) {
                invoke(noArguments.label(), false, object, List.of(), noArguments.params(), GenType.VOID);
                return;
            }
        }
    }

    // Allocates the object, runs the matching constructor on it and yields the new reference.
    public ExprValue newObject(ClassInfo classInfo, MethodInfo constructor, List<ExprValue> arguments) {
        String reference = quads.temp();
        quads.emit(QuadOp.NEW, Integer.toString(classInfo.sizeInBytes()), Integer.toString(classInfo.id()), reference);
        ExprValue object = new ExprValue(reference, GenType.object(classInfo.name()));
        construct(classInfo, constructor, object, arguments);
        return object;
    }

    public ExprValue loadField(ExprValue object, FieldInfo field) {
        String result = quads.emitTemp(QuadOp.LOAD_FIELD, object.operand(), Integer.toString(field.offset()));
        return new ExprValue(result, field.type());
    }

    public void storeField(ExprValue object, FieldInfo field, ExprValue value) {
        quads.emit(QuadOp.STORE_FIELD, value.operand(), object.operand(), Integer.toString(field.offset()));
    }

    // target is a label, or an operand holding the address of the routine when indirect is true.
    private ExprValue invoke(String target, boolean indirect, ExprValue receiver, List<ExprValue> arguments,
                             List<GenType> params, GenType returnType) {
        QuadOp call = indirect ? QuadOp.CALL_INDIRECT : QuadOp.CALL;
        int count = arguments.size() + (receiver == null ? 0 : 1);
        if (receiver != null) {
            quads.emit(QuadOp.PARAM, receiver.operand(), null, null);
        }
        for (int i = 0; i < arguments.size(); i++) {
            ExprValue argument = i < params.size() ? ops.coerce(arguments.get(i), params.get(i)) : arguments.get(i);
            quads.emit(QuadOp.PARAM, argument.operand(), null, null);
        }
        if (returnType.kind() == GenType.Kind.VOID) {
            quads.emit(call, target, Integer.toString(count), null);
            return new ExprValue(null, GenType.VOID);
        }
        String result = quads.temp();
        quads.emit(call, target, Integer.toString(count), result);
        return new ExprValue(result, returnType);
    }
}
