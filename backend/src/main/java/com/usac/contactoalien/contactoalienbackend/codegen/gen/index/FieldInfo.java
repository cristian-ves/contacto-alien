package com.usac.contactoalien.contactoalienbackend.codegen.gen.index;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;

// Instance field of a class or structure; offset is in bytes from the start of the heap object and
// length is the declared size of an array field (0 for any other field).
public record FieldInfo(String name, GenType type, int offset, int length) {

    public FieldInfo(String name, GenType type, int offset) {
        this(name, type, offset, 0);
    }
}
