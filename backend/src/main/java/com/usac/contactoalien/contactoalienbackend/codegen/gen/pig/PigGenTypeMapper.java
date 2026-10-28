package com.usac.contactoalien.contactoalienbackend.codegen.gen.pig;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;

// Maps Pig Latin basic type names to generator types and gives each its default initial value.
final class PigGenTypeMapper {

    private PigGenTypeMapper() {
    }

    static GenType map(PigParser.TipoBasicoContext type) {
        if (type.NUMERUS() != null) {
            return GenType.INT;
        }
        if (type.TEXTUM() != null) {
            return GenType.STRING;
        }
        if (type.LITTERA() != null) {
            return GenType.CHAR;
        }
        if (type.BOOL() != null) {
            return GenType.BOOL;
        }
        return GenType.FLOAT;
    }
}
