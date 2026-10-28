package com.usac.contactoalien.contactoalienbackend.codegen.gen.index;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;

import java.util.List;

/**
Signature of a Zetariano method or constructor (a constructor has the name "new" and a void result).
The key (name plus parameter type codes) is what an override repeats; a private method is never
dispatched dynamically.
*/
public record MethodInfo(String owner, String name, String label, List<GenType> params, GenType returnType,
                         boolean isPrivate) {

    public MethodInfo(String owner, String name, String label, List<GenType> params, GenType returnType) {
        this(owner, name, label, params, returnType, false);
    }

    public String key() {
        return name + "__" + params.stream().map(GenType::signatureCode).reduce("", String::concat);
    }
}
