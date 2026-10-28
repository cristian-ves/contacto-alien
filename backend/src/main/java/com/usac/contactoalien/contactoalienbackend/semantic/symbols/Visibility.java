package com.usac.contactoalien.contactoalienbackend.semantic.symbols;

/**
Access level of a Zetariano member. A member without modifier has DEFAULT (package) access.
*/
public enum Visibility {
    PUBLIC, PROTECTED, DEFAULT, PRIVATE;

    // Strength of the access: a larger value is more open. An override may not lower it.
    public int openness() {
        return switch (this) {
            case PUBLIC -> 3;
            case PROTECTED -> 2;
            case DEFAULT -> 1;
            case PRIVATE -> 0;
        };
    }

    // Name used in messages, as written in the source (default has no keyword).
    public String label() {
        return this == DEFAULT ? "default" : name().toLowerCase();
    }
}
