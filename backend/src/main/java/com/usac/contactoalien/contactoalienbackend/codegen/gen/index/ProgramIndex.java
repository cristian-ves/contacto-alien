package com.usac.contactoalien.contactoalienbackend.codegen.gen.index;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Everything the Pig program can call: the Y? functions and Zetariano classes it imports.
public final class ProgramIndex {

    private final Map<String, FunctionInfo> functions = new LinkedHashMap<>();
    private final Map<String, ClassInfo> classes = new LinkedHashMap<>();

    public void addFunction(FunctionInfo function) {
        functions.put(function.name(), function);
    }

    public void addClass(ClassInfo classInfo) {
        classes.put(classInfo.name(), classInfo);
    }

    public Collection<ClassInfo> classes() {
        return List.copyOf(classes.values());
    }

    // True when some class of the program declares a method with the same key as this one in a
    // subclass of its owner: only then does a call need to look the method up at run time.
    public boolean isOverridden(MethodInfo method) {
        ClassInfo owner = classes.get(method.owner());
        if (owner == null || method.isPrivate()) {
            return false;
        }
        return classes.values().stream()
                .anyMatch(other -> other != owner && other.isSubclassOf(owner) && other.declares(method.key()));
    }

    public FunctionInfo function(String name) {
        return functions.get(name);
    }

    public ClassInfo classInfo(String name) {
        return classes.get(name);
    }
}
