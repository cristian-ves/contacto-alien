package com.usac.contactoalien.contactoalienbackend.codegen.gen.y;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;

// Maps Y? type nodes to generator types; a structure name becomes an object type resolved through the index.
public final class YTypeMapper {

    private YTypeMapper() {
    }

    public static GenType map(YParser.TypeContext type) {
        if (type.IDENTIFIER() != null) {
            return GenType.object(type.IDENTIFIER().getText());
        }
        YParser.PrimitiveTypeContext primitive = type.primitiveType();
        if (primitive.ENTERO() != null) {
            return GenType.INT;
        }
        if (primitive.CADENA() != null) {
            return GenType.STRING;
        }
        if (primitive.CARACTER() != null) {
            return GenType.CHAR;
        }
        if (primitive.BOOL() != null) {
            return GenType.BOOL;
        }
        return GenType.FLOAT;
    }
}
