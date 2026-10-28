package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.analysis.AccessRules;
import com.usac.contactoalien.contactoalienbackend.semantic.analysis.ClassHierarchy;
import com.usac.contactoalien.contactoalienbackend.semantic.analysis.OverloadResolver;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ConstructorSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.MethodSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.VariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
Fields, methods and constructors of the imported Zetariano classes as seen from Pig Latin code.
The lookup follows the superclasses, picks the overload from the types of the arguments and only
lets Pig Latin use public members: a Pig program belongs to no class and no package.
*/
final class PigMemberResolver {

    private final CompilationUnitRegistry registry;
    private final PigErrorReporter reporter;
    private final ClassHierarchy hierarchy;
    private final AccessRules access;

    PigMemberResolver(PigImportResult importResult, PigErrorReporter reporter) {
        this.registry = importResult.getClassRegistry();
        this.reporter = reporter;
        this.hierarchy = new ClassHierarchy(registry);
        this.access = new AccessRules(registry);
    }

    PigType field(String className, String name, ParserRuleContext at) {
        ClassSymbolTable table = registry.resolve(className);
        VariableSymbol field = table == null ? null : hierarchy.findField(table, name);
        if (field == null) {
            reporter.report("field '" + name + "' is not declared in class '" + className + "'", at);
            return PigType.UNKNOWN;
        }
        ClassSymbolTable owner = registry.resolve(field.getOwnerClass());
        if (!access.canAccess(field.getVisibility(), owner, null)) {
            reporter.report("field '" + name + "' " + access.explain(field.getVisibility(), owner, null), at);
        }
        return PigType.fromZetariano(field.getType());
    }

    PigType call(String className, String name, List<PigType> arguments, ParserRuleContext at) {
        ClassSymbolTable table = registry.resolve(className);
        if (table == null) {
            reporter.report("class '" + className + "' is not known", at);
            return PigType.UNKNOWN;
        }
        List<MethodSymbol> candidates = hierarchy.findMethods(table, name);
        if (candidates.isEmpty()) {
            reporter.report("method '" + name + "' is not declared in class '" + className + "'", at);
            return PigType.UNKNOWN;
        }
        List<MethodSymbol> sameArity = candidates.stream().filter(m -> m.getArity() == arguments.size()).toList();
        if (sameArity.isEmpty()) {
            reporter.report("method '" + name + "' in class '" + className + "' does not accept " + arguments.size() + " argument(s)", at);
            return PigType.UNKNOWN;
        }
        List<ZType> argumentTypes = arguments.stream().map(PigType::toZetariano).toList();
        MethodSymbol chosen = OverloadResolver.pick(sameArity, MethodSymbol::getParameterTypes, argumentTypes, registry);
        if (chosen == null) {
            reporter.report("no overload of method '" + name + "' in class '" + className + "' accepts arguments "
                    + OverloadResolver.describe(argumentTypes), at);
            return PigType.UNKNOWN;
        }
        ClassSymbolTable owner = registry.resolve(chosen.getOwnerClass());
        if (!access.canAccess(chosen.getVisibility(), owner, null)) {
            reporter.report("method '" + name + "' " + access.explain(chosen.getVisibility(), owner, null), at);
        }
        return PigType.fromZetariano(chosen.getReturnType());
    }

    // `novus Name(args)`: the object type, or UNKNOWN after reporting why the construction is invalid.
    PigType construct(String className, List<PigType> arguments, ParserRuleContext at) {
        ClassSymbolTable table = registry.resolve(className);
        if (table == null) {
            reporter.report("class '" + className + "' is not declared", at);
            return PigType.UNKNOWN;
        }
        List<ConstructorSymbol> constructors = table.getConstructors();
        boolean valid = true;
        if (constructors.isEmpty()) {
            valid = arguments.isEmpty();
            if (!valid) {
                reporter.report("constructor of class '" + className + "' does not accept " + arguments.size() + " argument(s)", at);
            }
        } else {
            List<ConstructorSymbol> sameArity = constructors.stream().filter(c -> c.getArity() == arguments.size()).toList();
            List<ZType> argumentTypes = arguments.stream().map(PigType::toZetariano).toList();
            ConstructorSymbol chosen = sameArity.isEmpty() ? null
                    : OverloadResolver.pick(sameArity, ConstructorSymbol::getParameterTypes, argumentTypes, registry);
            if (sameArity.isEmpty()) {
                valid = false;
                reporter.report("constructor of class '" + className + "' does not accept " + arguments.size() + " argument(s)", at);
            } else if (chosen == null) {
                valid = false;
                reporter.report("no constructor of class '" + className + "' accepts arguments " + OverloadResolver.describe(argumentTypes), at);
            } else if (!access.canAccess(chosen.getVisibility(), table, null)) {
                valid = false;
                reporter.report("constructor of class '" + className + "' " + access.explain(chosen.getVisibility(), table, null), at);
            }
        }
        return valid ? PigType.ofObject(className) : PigType.UNKNOWN;
    }
}
