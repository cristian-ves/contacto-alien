package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.MethodSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.VariableSymbol;

import java.util.ArrayList;
import java.util.List;

/**
Lookups that follow the chain of superclasses: a class sees its own members first, then those
of its superclass, and so on. A method of a subclass hides the one of the superclass with the
same parameter types, which is how an override replaces the inherited method.
*/
public final class ClassHierarchy {

    private final CompilationUnitRegistry registry;

    public ClassHierarchy(CompilationUnitRegistry registry) {
        this.registry = registry;
    }

    // The class itself followed by its ancestors; stops at an unknown class or when the chain repeats.
    public List<ClassSymbolTable> chain(ClassSymbolTable start) {
        List<ClassSymbolTable> chain = new ArrayList<>();
        ClassSymbolTable current = start;
        while (current != null && !chain.contains(current)) {
            chain.add(current);
            current = current.getSuperName() == null ? null : registry.resolve(current.getSuperName());
        }
        return chain;
    }

    public VariableSymbol findField(ClassSymbolTable start, String name) {
        for (ClassSymbolTable table : chain(start)) {
            VariableSymbol field = table.resolveField(name);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    // Every method called name that the class can be asked for, without the ones an override hides.
    public List<MethodSymbol> findMethods(ClassSymbolTable start, String name) {
        List<MethodSymbol> found = new ArrayList<>();
        for (ClassSymbolTable table : chain(start)) {
            for (MethodSymbol method : table.resolveMethods(name)) {
                boolean hidden = found.stream().anyMatch(m -> m.getParameterTypes().equals(method.getParameterTypes()));
                if (!hidden) {
                    found.add(method);
                }
            }
        }
        return found;
    }

    // The method of an ancestor (not of the class itself) with this name and exactly these parameter types.
    public MethodSymbol findInherited(ClassSymbolTable start, MethodSymbol method) {
        List<ClassSymbolTable> chain = chain(start);
        for (ClassSymbolTable table : chain.subList(Math.min(1, chain.size()), chain.size())) {
            for (MethodSymbol candidate : table.resolveMethods(method.getName())) {
                if (candidate.getParameterTypes().equals(method.getParameterTypes())) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
