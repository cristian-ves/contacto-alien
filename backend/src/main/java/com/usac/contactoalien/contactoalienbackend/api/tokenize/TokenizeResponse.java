package com.usac.contactoalien.contactoalienbackend.api.tokenize;

import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;

import java.util.List;

public record TokenizeResponse(List<TokenDto> tokens, List<CompilerError> lexicalErrors) {
}