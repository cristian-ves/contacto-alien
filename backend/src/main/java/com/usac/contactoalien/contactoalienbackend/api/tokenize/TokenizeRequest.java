package com.usac.contactoalien.contactoalienbackend.api.tokenize;

// language must be exactly one of: "zetariano", "y", "pig"
public record TokenizeRequest(String code, String language) {
}