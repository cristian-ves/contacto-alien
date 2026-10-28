package com.usac.contactoalien.contactoalienbackend.codegen.gen.index;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
Layout and members of a Zetariano class or a Y? structure (which has id 0 and no methods). A heap object is a sequence of words: word 0 holds
the class id and the fields follow, those inherited from the superclass first (at the same offsets they have there) and then the
class's own. The class also keeps its method table: one slot per non-private method, inherited from the superclass and replaced
when the class overrides the method.
*/
public final class ClassInfo {

    private final String name;
    private final int id;
    private final ClassInfo parent;
    private final Map<String, FieldInfo> fields = new LinkedHashMap<>();
    private final List<MethodInfo> methods = new ArrayList<>();
    private final List<MethodInfo> constructors = new ArrayList<>();
    private final List<MethodInfo> vtable = new ArrayList<>();
    private final Map<String, Integer> slotByKey = new LinkedHashMap<>();
    private int fieldSlots;

    public ClassInfo(String name, int id) {
        this(name, id, null);
    }

    // A subclass starts with the fields and the method table of its superclass.
    public ClassInfo(String name, int id, ClassInfo parent) {
        this.name = name;
        this.id = id;
        this.parent = parent;
        if (parent != null) {
            fields.putAll(parent.fields);
            fieldSlots = parent.fieldSlots;
            vtable.addAll(parent.vtable);
            slotByKey.putAll(parent.slotByKey);
        }
    }

    public String name() {
        return name;
    }

    public int id() {
        return id;
    }

    public ClassInfo parent() {
        return parent;
    }

    public void addField(String fieldName, GenType type) {
        addField(fieldName, type, 0);
    }

    // length is the declared size of an array field (0 for any other field).
    public void addField(String fieldName, GenType type, int length) {
        int offset = (fieldSlots + 1) * TargetConfig.WORD_SIZE;
        fieldSlots++;
        fields.put(fieldName, new FieldInfo(fieldName, type, offset, length));
    }

    public List<FieldInfo> fields() {
        return List.copyOf(fields.values());
    }

    public void addMethod(MethodInfo method) {
        methods.add(method);
        if (method.isPrivate()) {
            return;
        }
        Integer slot = slotByKey.get(method.key());
        if (slot != null) {
            vtable.set(slot, method);
        } else {
            slotByKey.put(method.key(), vtable.size());
            vtable.add(method);
        }
    }

    public void addConstructor(MethodInfo constructor) {
        constructors.add(constructor);
    }

    public int sizeInBytes() {
        return (fieldSlots + 1) * TargetConfig.WORD_SIZE;
    }

    public FieldInfo field(String fieldName) {
        return fields.get(fieldName);
    }

    // The method table: entry k is the method that runs for slot k on an object of this class.
    public List<MethodInfo> vtable() {
        return List.copyOf(vtable);
    }

    // Slot of a non-private method (by its key), or -1.
    public int slotOf(String key) {
        return slotByKey.getOrDefault(key, -1);
    }

    public boolean declares(String key) {
        return methods.stream().anyMatch(method -> method.key().equals(key));
    }

    public boolean isSubclassOf(ClassInfo other) {
        for (ClassInfo current = this; current != null; current = current.parent) {
            if (current == other) {
                return true;
            }
        }
        return false;
    }

    // The methods called methodName that this class sees: its own, then the inherited ones that no
    // class below hides by repeating their key.
    public MethodInfo findMethod(String methodName, List<GenType> arguments) {
        List<MethodInfo> visible = new ArrayList<>();
        for (ClassInfo current = this; current != null; current = current.parent) {
            for (MethodInfo method : current.methods) {
                boolean hidden = visible.stream().anyMatch(seen -> seen.key().equals(method.key()));
                if (method.name().equals(methodName) && !hidden) {
                    visible.add(method);
                }
            }
        }
        return pick(visible, arguments);
    }

    public MethodInfo findConstructor(List<GenType> arguments) {
        return pick(constructors, arguments);
    }

    // Overload resolution at compile time: the candidate whose parameters need the fewest widenings
    // (an exact match costs 0); with no accepted candidate it falls back to the first of the same arity.
    private MethodInfo pick(List<MethodInfo> candidates, List<GenType> arguments) {
        MethodInfo best = null;
        MethodInfo sameArity = null;
        int bestCost = Integer.MAX_VALUE;
        for (MethodInfo candidate : candidates) {
            if (candidate.params().size() != arguments.size()) {
                continue;
            }
            if (sameArity == null) {
                sameArity = candidate;
            }
            int cost = costOf(candidate, arguments);
            if (cost >= 0 && cost < bestCost) {
                best = candidate;
                bestCost = cost;
            }
        }
        return best != null ? best : sameArity;
    }

    private static int costOf(MethodInfo candidate, List<GenType> arguments) {
        int total = 0;
        for (int i = 0; i < arguments.size(); i++) {
            int cost = arguments.get(i).conversionCost(candidate.params().get(i));
            if (cost < 0) {
                return -1;
            }
            total += cost;
        }
        return total;
    }
}
