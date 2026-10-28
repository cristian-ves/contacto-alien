package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.Visibility;

/**
Who may use a member, given the class that declares it (the owner) and the class whose code uses it.
public: everybody. private: only the owner. protected: the owner's package and its subclasses.
default (no modifier): the owner's package, except that a subclass of the owner may not use it,
not even from the same package. A default member used from a class of the same package that is not
a subclass is allowed. Code outside every class (Pig Latin) is treated as belonging to the package
of the classes it uses and as a subclass of none: Pig Latin programs read default fields of Zetariano
classes (for example the nodes of a linked list), so only private members are closed to it.
*/
public final class AccessRules {

    private final CompilationUnitRegistry registry;

    public AccessRules(CompilationUnitRegistry registry) {
        this.registry = registry;
    }

    // user is the class whose code makes the access, or null for Pig Latin code.
    public boolean canAccess(Visibility visibility, ClassSymbolTable owner, ClassSymbolTable user) {
        if (visibility == Visibility.PUBLIC) {
            return true;
        }
        if (owner == null) {
            return false;
        }
        if (user == null) {
            return visibility != Visibility.PRIVATE;
        }
        if (user.getClassName().equals(owner.getClassName())) {
            return true;
        }
        boolean samePackage = user.getPackageName().equals(owner.getPackageName());
        boolean subclass = registry.isSubclass(user.getClassName(), owner.getClassName());
        return switch (visibility) {
            case PRIVATE -> false;
            case PROTECTED -> samePackage || subclass;
            case DEFAULT -> samePackage && !subclass;
            case PUBLIC -> true;
        };
    }

    // Reason shown to the user when canAccess is false.
    public String explain(Visibility visibility, ClassSymbolTable owner, ClassSymbolTable user) {
        String who = user == null ? "Pig Latin code" : "class '" + user.getClassName() + "'";
        return switch (visibility) {
            case PRIVATE -> "is private to class '" + owner.getClassName() + "' and cannot be used from " + who;
            case PROTECTED -> "is protected in class '" + owner.getClassName()
                    + "' and can only be used from its package or its subclasses, not from " + who;
            case DEFAULT -> user != null && registry.isSubclass(user.getClassName(), owner.getClassName())
                    ? "has default access in class '" + owner.getClassName()
                            + "' and cannot be used from its subclass '" + user.getClassName() + "'"
                    : "has default access in class '" + owner.getClassName()
                            + "' and can only be used from its own package, not from " + who;
            case PUBLIC -> "";
        };
    }
}
