package com.usac.contactoalien.contactoalienbackend.api.compile;

import java.util.Map;

// Same shape as a validation request plus the text the program will read from standard input.
public record CompileRequest(String code, String language, Map<String, String> imports, String input) {
}
