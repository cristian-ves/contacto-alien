package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ConstructorSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.MethodSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.Visibility;
import com.usac.contactoalien.contactoalienbackend.semantic.types.TypeCompatibility;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import org.antlr.v4.runtime.Token;

import java.util.List;

/**
Class-level rules of inheritance, checked once per class before its bodies: the superclass must
exist and the chain must not loop; a method that repeats the signature of an inherited one must
carry @Override (the project makes the annotation mandatory), keep the return type and not lower
the visibility; @Override on a method that overrides nothing is an error; and a constructor that
does not start with super(...) calls super() implicitly, which needs a constructor without
parameters in the superclass.
*/
final class InheritanceChecker {

    private final ClassSymbolTable table;
    private final CompilationUnitRegistry registry;
    private final ErrorReporter errors;
    private final ClassHierarchy hierarchy;
    private final AccessRules access;

    InheritanceChecker(ClassSymbolTable table, CompilationUnitRegistry registry, ErrorReporter errors) {
        this.table = table;
        this.registry = registry;
        this.errors = errors;
        this.hierarchy = new ClassHierarchy(registry);
        this.access = new AccessRules(registry);
    }

    void check(ZetarianoParser.ClassDeclarationContext ctx) {
        boolean superclassOk = checkSuperclass(ctx);
        List<ZetarianoParser.MethodDeclarationContext> methodCtxs = ctx.classMember().stream()
                .map(ZetarianoParser.ClassMemberContext::methodDeclaration).filter(m -> m != null).toList();
        List<MethodSymbol> methods = table.getDeclaredMethods();
        for (int i = 0; i < methodCtxs.size() && i < methods.size(); i++) {
            checkMethod(methods.get(i), methodCtxs.get(i), superclassOk);
        }
        if (superclassOk) {
            checkImplicitSuperCalls(ctx);
        }
    }

    // False when the class has no usable superclass (none declared, unknown, itself or a cycle).
    private boolean checkSuperclass(ZetarianoParser.ClassDeclarationContext ctx) {
        String superName = table.getSuperName();
        if (superName == null) {
            return false;
        }
        Token at = ctx.IDENTIFIER(1).getSymbol();
        if (superName.equals(table.getClassName())) {
            errors.report("Class '" + superName + "' cannot extend itself", at);
            return false;
        }
        ClassSymbolTable superTable = registry.resolve(superName);
        if (superTable == null) {
            errors.report("Class '" + table.getClassName() + "' extends unknown class '" + superName + "'", at);
            return false;
        }
        if (registry.isSubclass(superName, table.getClassName())) {
            errors.report("Cyclic inheritance: class '" + table.getClassName() + "' is a superclass of itself through '"
                    + superName + "'", at);
            return false;
        }
        return true;
    }

    private void checkMethod(MethodSymbol method, ZetarianoParser.MethodDeclarationContext ctx, boolean superclassOk) {
        MethodSymbol inherited = superclassOk ? hierarchy.findInherited(table, method) : null;
        boolean overridable = inherited != null && inherited.getVisibility() != Visibility.PRIVATE;
        if (!overridable) {
            if (method.isOverrideAnnotated()) {
                errors.report("Method '" + method.getName() + "' is annotated with @Override but does not override any method of a superclass", ctx);
            }
            return;
        }
        String where = "'" + method.getName() + "' of class '" + inherited.getOwnerClass() + "'";
        if (!method.isOverrideAnnotated()) {
            errors.report("Method '" + method.getName() + "' overrides the method " + where + " and must be annotated with @Override", ctx);
        }
        if (!returnTypesMatch(method.getReturnType(), inherited.getReturnType())) {
            errors.report("Method '" + method.getName() + "' cannot override the method " + where + ": return type '"
                    + method.getReturnType() + "' is incompatible with '" + inherited.getReturnType() + "'", ctx);
        }
        if (method.getVisibility().openness() < inherited.getVisibility().openness()) {
            errors.report("Method '" + method.getName() + "' cannot override the method " + where + " with weaker access ("
                    + method.getVisibility().label() + " instead of " + inherited.getVisibility().label() + ")", ctx);
        }
    }

    // Same type, or a class that is a subclass of the inherited class return type.
    private boolean returnTypesMatch(ZType overriding, ZType inherited) {
        if (overriding.isUnknown() || inherited.isUnknown() || overriding.equals(inherited)) {
            return true;
        }
        boolean bothClasses = overriding.getKind() == ZType.Kind.CLASS && inherited.getKind() == ZType.Kind.CLASS
                && !overriding.isArray() && !inherited.isArray();
        return bothClasses && TypeCompatibility.isAssignable(inherited, overriding, registry);
    }

    // Constructors that do not open with super(...) call super() implicitly; so does the default constructor.
    private void checkImplicitSuperCalls(ZetarianoParser.ClassDeclarationContext ctx) {
        ClassSymbolTable superTable = registry.resolve(table.getSuperName());
        List<ZetarianoParser.ConstructorDeclarationContext> constructors = ctx.classMember().stream()
                .map(ZetarianoParser.ClassMemberContext::constructorDeclaration).filter(c -> c != null).toList();
        if (constructors.isEmpty()) {
            checkNoArgumentSuperConstructor(superTable, ctx.IDENTIFIER(0).getSymbol());
            return;
        }
        for (ZetarianoParser.ConstructorDeclarationContext constructor : constructors) {
            List<ZetarianoParser.StatementContext> body = constructor.block().statement();
            boolean callsSuper = !body.isEmpty() && SuperCallShape.isSuperConstructorCall(body.get(0));
            if (!callsSuper) {
                checkNoArgumentSuperConstructor(superTable, constructor.IDENTIFIER().getSymbol());
            }
        }
    }

    private void checkNoArgumentSuperConstructor(ClassSymbolTable superTable, Token at) {
        List<ConstructorSymbol> constructors = superTable.getConstructors();
        if (constructors.isEmpty()) {
            return;
        }
        ConstructorSymbol noArguments = constructors.stream().filter(c -> c.getArity() == 0).findFirst().orElse(null);
        if (noArguments == null) {
            errors.report("Class '" + table.getClassName() + "' must call super(...) explicitly: its superclass '"
                    + superTable.getClassName() + "' has no constructor without parameters", at);
        } else if (!access.canAccess(noArguments.getVisibility(), superTable, table)) {
            errors.report("The implicit super() of class '" + table.getClassName() + "' uses a constructor of '"
                    + superTable.getClassName() + "' that " + access.explain(noArguments.getVisibility(), superTable, table), at);
        }
    }
}
