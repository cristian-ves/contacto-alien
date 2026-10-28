package com.usac.contactoalien.contactoalienbackend.codegen.gen;

// Something that can be read and assigned: a variable, a field of an object or an array element.
public interface Target {

    GenType type();

    ExprValue read();

    // Stores the value (converted to the type of the target) and returns what was stored.
    ExprValue write(ExprValue value);
}
