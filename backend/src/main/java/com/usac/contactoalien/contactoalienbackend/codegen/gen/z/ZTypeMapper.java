package com.usac.contactoalien.contactoalienbackend.codegen.gen.z;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;

// Maps Zetariano type nodes to generator types; each pair of brackets wraps the type in one array level.
public final class ZTypeMapper {

    private ZTypeMapper() {
    }

    public static GenType map(ZetarianoParser.TypeContext type) {
        GenType mapped = named(type.INT() != null, type.DOUBLE() != null, type.CHAR() != null,
                type.BOOLEAN() != null, type.STRING() != null, type.IDENTIFIER() == null ? null : type.IDENTIFIER().getText());
        return wrap(mapped, type.LBRACKET().size());
    }

    // Element type of `new T[a][b]`: the type named after `new`, before any array level.
    static GenType leaf(ZetarianoParser.ArrayCreationContext creation) {
        return named(creation.INT() != null, creation.DOUBLE() != null, creation.CHAR() != null,
                creation.BOOLEAN() != null, creation.STRING() != null,
                creation.IDENTIFIER() == null ? null : creation.IDENTIFIER().getText());
    }

    static GenType wrap(GenType base, int levels) {
        GenType result = base;
        for (int i = 0; i < levels; i++) {
            result = GenType.array(result);
        }
        return result;
    }

    // A null type node means `void` in a method declaration.
    public static GenType mapReturn(ZetarianoParser.TypeContext type) {
        return type == null ? GenType.VOID : map(type);
    }

    private static GenType named(boolean isInt, boolean isDouble, boolean isChar, boolean isBoolean,
                                 boolean isString, String className) {
        if (isInt) {
            return GenType.INT;
        }
        if (isDouble) {
            return GenType.FLOAT;
        }
        if (isChar) {
            return GenType.CHAR;
        }
        if (isBoolean) {
            return GenType.BOOL;
        }
        return isString ? GenType.STRING : GenType.object(className);
    }
}
