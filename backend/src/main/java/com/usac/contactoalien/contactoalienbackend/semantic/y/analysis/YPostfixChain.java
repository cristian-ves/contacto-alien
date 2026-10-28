package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.*;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;

import java.util.List;
import java.util.function.Function;

// Types the operators that follow a primary expression (.field, [index], (call), ++, --), threading the
// type through the chain and reporting what cannot be applied to it.
final class YPostfixChain {

    private final YStructRegistry structRegistry;
    private final YErrorReporter errors;
    private final Function<ExpressionContext, YType> expressions;

    YPostfixChain(YStructRegistry structRegistry, YErrorReporter errors, Function<ExpressionContext, YType> expressions) {
        this.structRegistry = structRegistry;
        this.errors = errors;
        this.expressions = expressions;
    }

    YType continueFrom(YType startType, PrimaryContext primary, List<PostfixOpContext> ops, int startIndex, int declaredLength) {
        YType currentType = startType;
        for (int i = startIndex; i < ops.size(); i++) {
            PostfixOpContext op = ops.get(i);
            if (op.DOT() != null) {
                String fieldName = op.IDENTIFIER().getText();
                if (currentType.isUnknown()) {
                    currentType = YType.UNKNOWN;
                } else if (currentType.getKind() != YType.Kind.STRUCT || currentType.isArray()) {
                    errors.report("Cannot access '" + fieldName + "' on a value of type '" + currentType + "', which is not a known structure instance", op);
                    currentType = YType.UNKNOWN;
                } else {
                    YStructSymbol struct = structRegistry.resolve(currentType.getStructName());
                    YVariableSymbol field = struct.resolveField(fieldName);
                    if (field == null) {
                        errors.report("Structure '" + struct.getName() + "' has no field named '" + fieldName + "'", op);
                        currentType = YType.UNKNOWN;
                    } else {
                        currentType = field.getType();
                    }
                }
            } else if (op.LPAREN() != null) {
                // a call not immediately preceded by an identifier: Y? has no first-class functions
                if (!currentType.isUnknown()) {
                    errors.report("Expression is not callable", op);
                }
                currentType = YType.UNKNOWN;
            } else if (op.LBRACK() != null) {
                YType indexType = expressions.apply(op.expression());
                if (!indexType.isUnknown() && indexType.getKind() != YType.Kind.ENTERO) {
                    errors.report("Array index must be of type entero, found '" + indexType + "'", op);
                }
                if (i == 0) {
                    checkConstantIndex(op.expression(), declaredLength, op);
                }
                if (currentType.isArray()) {
                    currentType = currentType.asArray(-1);
                } else if (!currentType.isUnknown()) {
                    errors.report("Cannot index a value of type '" + currentType + "'; it is not an array", op);
                    currentType = YType.UNKNOWN;
                }
            } else {
                // INC or DEC postfix operator
                if (!YPlaceRules.isPlace(primary, ops, i)) {
                    errors.report("Operand of '" + op.getText() + "' must be a variable, a structure field or an array element", op);
                }
                if (!currentType.isUnknown() && !currentType.isNumeric()) {
                    errors.report("Postfix operator '" + op.getText() + "' requires a numeric operand, found '" + currentType + "'", op);
                }
            }
        }
        return currentType;
    }

    // A literal index is checked against the declared size as soon as it is known.
    private void checkConstantIndex(ExpressionContext index, int declaredLength, PostfixOpContext at) {
        if (declaredLength < 0) {
            return;
        }
        long value;
        try {
            value = Long.parseLong(index.getText());
        } catch (NumberFormatException notLiteral) {
            return;
        }
        if (value < 0 || value >= declaredLength) {
            errors.report("Array index " + value + " is outside the declared range 0.." + (declaredLength - 1), at);
        }
    }
}
