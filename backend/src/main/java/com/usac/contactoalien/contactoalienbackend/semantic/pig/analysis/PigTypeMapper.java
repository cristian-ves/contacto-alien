package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;

// Small bridge from the tipoBasico grammar rule (used in basicVarDecl, arrayDecl and forInit)
// to its PigType equivalent.
final class PigTypeMapper {

    private PigTypeMapper() {}

    static PigType mapTipoBasico(PigParser.TipoBasicoContext ctx) {
        if (ctx.NUMERUS() != null) return PigType.NUMERUS;
        if (ctx.DECIMALIS() != null) return PigType.DECIMALIS;
        if (ctx.TEXTUM() != null) return PigType.TEXTUM;
        if (ctx.LITTERA() != null) return PigType.LITTERA;
        if (ctx.BOOL() != null) return PigType.BOOL;
        return PigType.UNKNOWN;
    }
}