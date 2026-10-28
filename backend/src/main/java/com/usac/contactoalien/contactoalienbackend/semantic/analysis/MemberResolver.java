package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ConstructorSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.MethodSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.VariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
Finds the fields, methods and constructors an expression refers to, searching the class and its
superclasses and checking that the class under analysis is allowed to use what it finds. Every
problem is reported once and the lookup still returns the member it found (when there is one) so
the caller keeps a real type and does not cascade further errors.
*/
final class MemberResolver {

    private final ClassSymbolTable current;
    private final CompilationUnitRegistry registry;
    private final ErrorReporter errors;
    private final ClassHierarchy hierarchy;
    private final AccessRules access;

    MemberResolver(ClassSymbolTable current, CompilationUnitRegistry registry, ErrorReporter errors) {
        this.current = current;
        this.registry = registry;
        this.errors = errors;
        this.hierarchy = new ClassHierarchy(registry);
        this.access = new AccessRules(registry);
    }

    // The table of a class: the one under analysis, or the one registered under that name.
    ClassSymbolTable tableOf(String className) {
        return className.equals(current.getClassName()) ? current : registry.resolve(className);
    }

    ClassSymbolTable superTable() {
        return current.getSuperName() == null ? null : tableOf(current.getSuperName());
    }

    // A field named without receiver: declared by this class or inherited. Null when there is none.
    VariableSymbol bareField(String name, ParserRuleContext at) {
        VariableSymbol field = hierarchy.findField(current, name);
        if (field != null) {
            checkFieldAccess(field, at);
        }
        return field;
    }

    // A field reached with `receiver.name`.
    VariableSymbol field(ClassSymbolTable target, String name, ParserRuleContext at) {
        VariableSymbol field = hierarchy.findField(target, name);
        if (field == null) {
            errors.report("Class '" + target.getClassName() + "' has no field named '" + name + "'", at);
            return null;
        }
        checkFieldAccess(field, at);
        return field;
    }

    ZType callMethod(ClassSymbolTable target, String name, List<ZType> argTypes, ParserRuleContext at) {
        List<MethodSymbol> candidates = hierarchy.findMethods(target, name);
        if (candidates.isEmpty()) {
            errors.report("Method '" + name + "' is not declared in class '" + target.getClassName() + "'", at);
            return ZType.UNKNOWN;
        }
        List<MethodSymbol> sameArity = candidates.stream().filter(c -> c.getArity() == argTypes.size()).toList();
        if (sameArity.isEmpty()) {
            errors.report("Method '" + name + "' called with " + argTypes.size() + " argument(s), but no overload with that many parameters exists", at);
            return ZType.UNKNOWN;
        }
        MethodSymbol chosen = OverloadResolver.pick(sameArity, MethodSymbol::getParameterTypes, argTypes, registry);
        if (chosen == null) {
            errors.report("No overload of method '" + name + "' accepts arguments " + OverloadResolver.describe(argTypes), at);
            return ZType.UNKNOWN;
        }
        ClassSymbolTable owner = chosen.getOwnerClass() == null ? target : tableOf(chosen.getOwnerClass());
        if (!access.canAccess(chosen.getVisibility(), owner, current)) {
            errors.report("Method '" + name + "' " + access.explain(chosen.getVisibility(), owner, current), at);
        }
        return chosen.getReturnType();
    }

    // `new Name(args)`.
    ZType construct(String className, List<ZType> argTypes, ParserRuleContext at) {
        ClassSymbolTable target = tableOf(className);
        if (target == null) {
            errors.report("Unknown class '" + className + "'; no class named '" + className + "' is declared or registered", at);
            return ZType.UNKNOWN;
        }
        checkConstructor(target, argTypes, at, "Class '" + className + "' has no declared constructor; only no-argument construction is allowed");
        return ZType.ofClass(className);
    }

    // `super(args)` inside a constructor of a subclass.
    void superConstructor(List<ZType> argTypes, ParserRuleContext at) {
        ClassSymbolTable target = superTable();
        if (target != null) {
            checkConstructor(target, argTypes, at, "Class '" + target.getClassName() + "' has no declared constructor; only super() is allowed");
        }
    }

    private void checkConstructor(ClassSymbolTable target, List<ZType> argTypes, ParserRuleContext at, String noConstructorMessage) {
        String className = target.getClassName();
        List<ConstructorSymbol> constructors = target.getConstructors();
        if (constructors.isEmpty()) {
            // no explicit constructor declared: assume an implicit no-argument constructor, as in Java
            if (!argTypes.isEmpty()) {
                errors.report(noConstructorMessage, at);
            }
            return;
        }
        List<ConstructorSymbol> sameArity = constructors.stream().filter(c -> c.getArity() == argTypes.size()).toList();
        if (sameArity.isEmpty()) {
            errors.report("No constructor of '" + className + "' accepts " + argTypes.size() + " argument(s)", at);
            return;
        }
        ConstructorSymbol chosen = OverloadResolver.pick(sameArity, ConstructorSymbol::getParameterTypes, argTypes, registry);
        if (chosen == null) {
            errors.report("No constructor of '" + className + "' accepts arguments " + OverloadResolver.describe(argTypes), at);
        } else if (!access.canAccess(chosen.getVisibility(), target, current)) {
            errors.report("Constructor of '" + className + "' " + access.explain(chosen.getVisibility(), target, current), at);
        }
    }

    private void checkFieldAccess(VariableSymbol field, ParserRuleContext at) {
        ClassSymbolTable owner = field.getOwnerClass() == null ? current : tableOf(field.getOwnerClass());
        if (!access.canAccess(field.getVisibility(), owner, current)) {
            errors.report("Field '" + field.getName() + "' " + access.explain(field.getVisibility(), owner, current), at);
        }
    }
}
