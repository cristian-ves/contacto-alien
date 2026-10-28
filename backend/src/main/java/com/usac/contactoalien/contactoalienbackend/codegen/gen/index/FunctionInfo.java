package com.usac.contactoalien.contactoalienbackend.codegen.gen.index;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;

import java.util.List;

// Signature of a Y? function as seen by its callers; label is the assembly symbol.
public record FunctionInfo(String name, String label, List<GenType> params, GenType returnType) {
}
