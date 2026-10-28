package com.usac.contactoalien.contactoalienbackend.api.validate;

import java.util.Map;

/**
A file to validate. For a Zetariano class, imports holds the other classes of its project keyed by
path and path is the location of the file itself, so its package (its folder) is known; without
it the file is assumed to sit with the classes it was sent with.
*/
public record ValidateRequest(String code, String language, Map<String, String> imports, String path) {

    public ValidateRequest(String code, String language, Map<String, String> imports) {
        this(code, language, imports, null);
    }
}
