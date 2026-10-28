package com.usac.contactoalien.contactoalienbackend.codegen.quad;

// Creates fresh temporaries (t0, t1...) and labels (L0, L1...) and keeps source names from colliding with them.
public final class NameFactory {

    private int temporaries;
    private int labels;

    public String temp() {
        return "t" + temporaries++;
    }

    public String label() {
        return "L" + labels++;
    }

    // A source variable called like a temporary (t12) gets a suffix so both can coexist.
    public String variable(String sourceName) {
        return looksLikeTemp(sourceName) ? sourceName + "_v" : sourceName;
    }

    private boolean looksLikeTemp(String name) {
        if (name.length() < 2 || name.charAt(0) != 't') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
