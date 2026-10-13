package com.usac.contactoalien.contactoalienbackend.api.validate;

import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;

import java.util.List;

public record ValidateResponse(boolean valid, List<CompilerError> errors) {
}