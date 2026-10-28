package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigTypeCompatibility;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
Rules for the pieces that hold several values: series (length, initial values, index type),
structure initializers (type of each positional value) and the variables that `<<` can fill.
The messages live here so the three analyzers that call it stay short.
*/
final class PigCollectionRules {

    private final PigErrorReporter reporter;

    PigCollectionRules(PigErrorReporter reporter) {
        this.reporter = reporter;
    }

    void checkSeriesLength(PigParser.ArrayDeclContext ctx) {
        String size = ctx.INT_LITERAL().getText();
        int length = IntegerLiterals.length(size);
        if (IntegerLiterals.isLengthTooLarge(size)) {
            reporter.report(IntegerLiterals.lengthOutOfRange(size), ctx);
        } else if (length < 1) {
            reporter.report("series '" + ctx.IDENTIFIER(0).getText() + "' must have at least 1 element", ctx);
        } else if (ctx.expression().size() > length) {
            reporter.report("series '" + ctx.IDENTIFIER(0).getText() + "' has " + length + " element(s) but "
                    + ctx.expression().size() + " value(s) were given", ctx);
        }
    }

    void checkIndex(PigType indexType, ParserRuleContext at) {
        if (!indexType.isUnknown() && indexType.getKind() != PigType.Kind.NUMERUS) {
            reporter.report("series index must be numerus, found " + indexType, at);
        }
    }

    // A literal index is checked against the declared size as soon as it is known, as the series rules ask.
    void checkConstantIndex(ParserRuleContext index, int declaredLength) {
        if (declaredLength < 0) return;
        long value;
        try {
            value = Long.parseLong(index.getText());
        } catch (NumberFormatException notLiteral) {
            return;
        }
        if (value < 0 || value >= declaredLength) {
            reporter.report("series index " + value + " is outside the declared range 0.." + (declaredLength - 1), index);
        }
    }

    void checkReadable(String name, PigType type, ParserRuleContext at) {
        switch (type.getKind()) {
            case NUMERUS, DECIMALIS, TEXTUM, LITTERA, BOOL, UNKNOWN -> { }
            default -> reporter.report("cannot read into '" + name + "' of type " + type
                    + "; use a numerus, decimalis, textum, littera or bool variable", at);
        }
    }

    // Each positional value must fit its field; an array field takes a series of the same element type.
    void checkStructValues(YStructSymbol struct, List<PigType> values, ParserRuleContext at) {
        int position = 0;
        for (YVariableSymbol field : struct.getFieldsInOrder()) {
            if (position >= values.size()) {
                return;
            }
            PigType expected = fieldType(field.getType());
            PigType given = values.get(position++);
            if (!PigTypeCompatibility.isAssignable(expected, given)) {
                reporter.report("value " + position + " of structure '" + struct.getName() + "' (field '"
                        + field.getName() + "') must be " + expected + ", found " + given, at);
            }
        }
    }

    private PigType fieldType(YType type) {
        return PigType.fromY(type);
    }
}
