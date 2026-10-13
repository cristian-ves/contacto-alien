package com.usac.contactoalien.contactoalienbackend.api.validate;

import java.util.Map;

public record ValidateRequest(String code, String language, Map<String, String> imports) {
}