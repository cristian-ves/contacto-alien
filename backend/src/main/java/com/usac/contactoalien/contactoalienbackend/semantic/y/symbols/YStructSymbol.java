package com.usac.contactoalien.contactoalienbackend.semantic.y.symbols;

import java.util.LinkedHashMap;
import java.util.Map;

public final class YStructSymbol {
    private final String name;
    private final Map<String, YVariableSymbol> fields = new LinkedHashMap<>();

    public YStructSymbol(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public boolean declareField(YVariableSymbol field) {
        if (fields.containsKey(field.getName())) return false;
        fields.put(field.getName(), field);
        return true;
    }

    public YVariableSymbol resolveField(String name) { return fields.get(name); }

    public int getFieldCount() { return fields.size(); }

    public java.util.Collection<YVariableSymbol> getFieldsInOrder() {
        return java.util.Collections.unmodifiableCollection(fields.values());
    }

}