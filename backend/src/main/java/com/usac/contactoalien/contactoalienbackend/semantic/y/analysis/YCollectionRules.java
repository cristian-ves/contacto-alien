package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.VariableDeclarationContext;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YFunctionSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeCompatibility;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

/**
Rules for arrays and for the arguments of a call: declared length, the values of a `{..}`
list, and the type of each argument against its parameter (arrays and structures are only
accepted when they have exactly the parameter's type, because they are passed by reference).
*/
final class YCollectionRules {

    private final YErrorReporter errors;

    YCollectionRules(YErrorReporter errors) {
        this.errors = errors;
    }

    void checkLength(VariableDeclarationContext ctx) {
        for (org.antlr.v4.runtime.tree.TerminalNode size : ctx.INT_LITERAL()) {
            if (IntegerLiterals.isLengthTooLarge(size.getText())) {
                errors.report(IntegerLiterals.lengthOutOfRange(size.getText()), ctx);
                return;
            }
            if (IntegerLiterals.length(size.getText()) < 1) {
                errors.report("Array '" + ctx.IDENTIFIER().getText() + "' must have at least 1 element", ctx);
                return;
            }
        }
    }

    // Each value of `tipo a[n] = {..}` must fit the element type, and there cannot be more than n.
    void checkInitializer(VariableDeclarationContext ctx, YType element, List<YType> values) {
        int length = IntegerLiterals.length(ctx.INT_LITERAL(0).getText());
        if (values.size() > length) {
            errors.report("Array '" + ctx.IDENTIFIER().getText() + "' has " + length + " element(s) but "
                    + values.size() + " value(s) were given", ctx);
        }
        for (YType value : values) {
            if (!YTypeCompatibility.isAssignable(element, value)) {
                errors.report("Cannot store a value of type '" + value + "' in an array of '" + element + "'", ctx);
            }
        }
    }

    // `P p = {v0, v1}`: one value per field, in order, each assignable to its field.
    void checkStructList(YStructSymbol struct, List<YType> values, ParserRuleContext at) {
        if (values.size() != struct.getFieldCount()) {
            errors.report("Structure '" + struct.getName() + "' has " + struct.getFieldCount() + " field(s) but "
                    + values.size() + " value(s) were given", at);
            return;
        }
        int position = 0;
        for (YVariableSymbol field : struct.getFieldsInOrder()) {
            YType value = values.get(position++);
            if (!YTypeCompatibility.isAssignable(field.getType(), value)) {
                errors.report("Value " + position + " of structure '" + struct.getName() + "' (field '"
                        + field.getName() + "') must be '" + field.getType() + "', found '" + value + "'", at);
            }
        }
    }

    // Type of a `{..}` list used as an expression: an array when all values share a scalar type
    // (decimal if any is), otherwise undetermined because it may be a structure value list.
    YType listType(List<YType> values) {
        YType element = null;
        for (YType value : values) {
            if (value.isUnknown() || value.isArray() || value.getKind() == YType.Kind.STRUCT) {
                return YType.UNKNOWN;
            }
            if (element == null || (element.getKind() == YType.Kind.ENTERO && value.getKind() == YType.Kind.FLOTANTE)) {
                element = value;
            } else if (!YTypeCompatibility.isAssignable(element, value)) {
                return YType.UNKNOWN;
            }
        }
        return element == null ? YType.UNKNOWN : element.asArray(1);
    }

    void checkArguments(YFunctionSymbol function, List<YType> arguments, ParserRuleContext at) {
        List<YType> parameters = function.getParameterTypes();
        for (int i = 0; i < arguments.size() && i < parameters.size(); i++) {
            if (!YTypeCompatibility.isAssignable(parameters.get(i), arguments.get(i))) {
                errors.report("Argument " + (i + 1) + " of function '" + function.getName() + "' must be '"
                        + parameters.get(i) + "', found '" + arguments.get(i) + "'", at);
            }
        }
    }
}
