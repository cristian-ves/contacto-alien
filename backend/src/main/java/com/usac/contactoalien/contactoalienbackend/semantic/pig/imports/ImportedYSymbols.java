package com.usac.contactoalien.contactoalienbackend.semantic.pig.imports;

import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YFunctionRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YFunctionSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Accumulates the structures and functions declared across every .y file imported by one Pig
// Latin file. A name already added by a previous import that reappears in a later import is a
// collision error (two different .y files cannot both contribute a symbol of the same name).
public final class ImportedYSymbols {

    private final Map<String, YStructSymbol> structs = new LinkedHashMap<>();
    private final Map<String, YFunctionSymbol> functions = new LinkedHashMap<>();

    // structNames/functionNames are the names declared in ONE .y file, already known to the
    // caller (collected from that file's own parse tree); each is resolved against that same
    // file's own registries and merged in here. Returns collision messages, without any file
    // prefix (the caller attaches which file it came from).
    public List<String> addFrom(YStructRegistry structRegistry, List<String> structNames,
                                YFunctionRegistry functionRegistry, List<String> functionNames) {
        List<String> collisions = new ArrayList<>();

        for (String name : structNames) {
            YStructSymbol symbol = structRegistry.resolve(name);
            if (symbol == null) continue;
            if (structs.containsKey(name)) {
                collisions.add("Structure '" + name + "' collides with a structure of the same name already imported from another file");
            } else {
                structs.put(name, symbol);
            }
        }

        for (String name : functionNames) {
            YFunctionSymbol symbol = functionRegistry.resolve(name);
            if (symbol == null) continue;
            if (functions.containsKey(name)) {
                collisions.add("Function '" + name + "' collides with a function of the same name already imported from another file");
            } else {
                functions.put(name, symbol);
            }
        }

        return collisions;
    }

    public YStructSymbol resolveStruct(String name) { return structs.get(name); }
    public YFunctionSymbol resolveFunction(String name) { return functions.get(name); }
    public Map<String, YStructSymbol> getStructs() { return Collections.unmodifiableMap(structs); }
    public Map<String, YFunctionSymbol> getFunctions() { return Collections.unmodifiableMap(functions); }
}