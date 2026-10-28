package com.usac.contactoalien.contactoalienbackend.semantic.imports;

import java.util.Map;

/**
A class belongs to the package named by the folder of its file, so two classes share a package
when their paths have the same directory part ("banco/Cuenta.z" and "banco/Banco.z" do).
*/
public final class PackagePaths {

    private PackagePaths() {
    }

    // Directory part of a virtual path, or "" for a file at the root.
    public static String of(String path) {
        if (path == null) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    // The file being validated has no path of its own: it sits with the sibling classes it was sent with.
    public static String ofSiblings(Map<String, String> imports) {
        if (imports == null || imports.isEmpty()) {
            return "";
        }
        return of(imports.keySet().iterator().next());
    }
}
