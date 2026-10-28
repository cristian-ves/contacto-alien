package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.*;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.VariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.List;
import java.util.function.Function;

// Types the operators that follow a primary expression (.member, [index], (call), ++, --) and the
// `super` forms, threading the type through the chain. Nested expressions go back through the callbacks.
final class PostfixChainResolver {

    // Arrays expose only this read-only member, as in Java.
    private static final String ARRAY_LENGTH = "length";

    private final ClassSymbolTable classSymbolTable;
    private final MemberResolver members;
    private final ErrorReporter errors;
    private final Function<ExpressionContext, ZType> expressions;
    private final Function<ArgumentListContext, List<ZType>> arguments;
    // True when the last chain resolved ended in `array.length`, which cannot be assigned.
    private boolean endsInArrayLength;

    PostfixChainResolver(ClassSymbolTable classSymbolTable, MemberResolver members, ErrorReporter errors,
                         Function<ExpressionContext, ZType> expressions,
                         Function<ArgumentListContext, List<ZType>> arguments) {
        this.classSymbolTable = classSymbolTable;
        this.members = members;
        this.errors = errors;
        this.expressions = expressions;
        this.arguments = arguments;
    }

    boolean endsInArrayLength() {
        return endsInArrayLength;
    }

    // `super(...)`, `super.method(...)` and `super.field`: only valid in a subclass, always against the superclass.
    ZType resolveSuper(PrimaryExpressionContext primary, List<PostfixOpContext> ops,
                      boolean superConstructorCallAllowed) {
        ClassSymbolTable superTable = members.superTable();
        if (classSymbolTable.getSuperName() == null) {
            errors.report("'super' can only be used in a class that extends another class", primary);
            return ZType.UNKNOWN;
        }
        if (ops.isEmpty()) {
            errors.report("'super' must be followed by a call or a member access", primary);
            return ZType.UNKNOWN;
        }
        PostfixOpContext first = ops.get(0);
        if (first.LPAREN() != null) {
            List<ZType> argTypes = arguments.apply(first.argumentList());
            if (!superConstructorCallAllowed) {
                errors.report("Call to super(...) must be the first statement of a constructor", primary);
            } else if (superTable != null) {
                members.superConstructor(argTypes, primary);
            }
            return continueFrom(ZType.VOID, ops, 1);
        }
        if (first.DOT() == null || superTable == null) {
            errors.report("'super' must be followed by a call or a member access", primary);
            return ZType.UNKNOWN;
        }
        String memberName = first.IDENTIFIER().getText();
        boolean isCall = ops.size() > 1 && ops.get(1).LPAREN() != null;
        if (isCall) {
            List<ZType> argTypes = arguments.apply(ops.get(1).argumentList());
            return continueFrom(members.callMethod(superTable, memberName, argTypes, first), ops, 2);
        }
        VariableSymbol field = members.field(superTable, memberName, first);
        return continueFrom(field == null ? ZType.UNKNOWN : field.getType(), ops, 1);
    }

    ZType continueFrom(ZType startType, List<PostfixOpContext> ops, int startIndex) {
        ZType currentType = startType;
        int i = startIndex;
        endsInArrayLength = false;
        while (i < ops.size()) {
            PostfixOpContext op = ops.get(i);
            boolean lengthOp = false;
            if (op.DOT() != null) {
                String memberName = op.IDENTIFIER().getText();
                boolean nextIsCall = (i + 1 < ops.size()) && ops.get(i + 1).LPAREN() != null;
                if (currentType.isUnknown()) {
                    currentType = ZType.UNKNOWN;
                } else if (currentType.isArray() && memberName.equals(ARRAY_LENGTH) && !nextIsCall) {
                    currentType = ZType.INT;
                    lengthOp = true;
                } else if (currentType.isArray() || currentType.getKind() != ZType.Kind.CLASS) {
                    errors.report("Cannot access '" + memberName + "' on a value of type '" + currentType + "', which is not an object of a known class", op);
                    currentType = ZType.UNKNOWN;
                } else {
                    ClassSymbolTable targetTable = members.tableOf(currentType.getClassName());
                    if (nextIsCall) {
                        List<ZType> argTypes = arguments.apply(ops.get(i + 1).argumentList());
                        currentType = members.callMethod(targetTable, memberName, argTypes, op);
                        i++; // the following LPAREN op belongs to this same call, consume it too
                    } else {
                        VariableSymbol field = targetTable == null ? null : members.field(targetTable, memberName, op);
                        currentType = field == null ? ZType.UNKNOWN : field.getType();
                    }
                }
            } else if (op.LPAREN() != null) {
                // a call not immediately preceded by an identifier/DOT: this language has no first-class functions
                if (!currentType.isUnknown()) {
                    errors.report("Expression is not callable", op);
                }
                currentType = ZType.UNKNOWN;
            } else if (op.LBRACKET() != null) {
                ZType indexType = expressions.apply(op.expression());
                if (!indexType.isUnknown() && indexType.getKind() != ZType.Kind.INT) {
                    errors.report("Array index must be of type int, found '" + indexType + "'", op);
                }
                if (currentType.isArray()) {
                    currentType = currentType.asArray(-1);
                } else if (!currentType.isUnknown()) {
                    errors.report("Cannot index a value of type '" + currentType + "'; it is not an array", op);
                    currentType = ZType.UNKNOWN;
                }
            } else {
                // postfix ++ or --
                if (!ZPlaceRules.isPlaceBefore(op)) {
                    errors.report("Operand of '" + op.getText() + "' must be a variable, a field or an array element", op);
                }
                if (!currentType.isUnknown() && !currentType.isNumeric()) {
                    errors.report("Postfix operator '" + op.getText() + "' requires a numeric operand, found '" + currentType + "'", op);
                }
            }
            endsInArrayLength = lengthOp;
            i++;
        }
        return currentType;
    }
}
