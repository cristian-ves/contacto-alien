package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.*;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.*;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeCompatibility;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

// Computes the YType of any expression subtree, reporting every semantic error found along the
// way and returning YType.UNKNOWN to suppress cascading errors on that branch.
public final class YExpressionTypeResolver {

    private final YStructRegistry structRegistry;
    private final YFunctionRegistry functionRegistry;
    private final YErrorReporter errors;
    private YScope currentScope;

    public YExpressionTypeResolver(YStructRegistry structRegistry, YFunctionRegistry functionRegistry, YErrorReporter errors) {
        this.structRegistry = structRegistry;
        this.functionRegistry = functionRegistry;
        this.errors = errors;
    }

    public void setScope(YScope scope) {
        this.currentScope = scope;
    }

    public YType resolve(ExpressionContext ctx) {
        return resolveAssignment(ctx.assignment());
    }

    private YType resolveAssignment(AssignmentContext ctx) {
        YType leftType = resolveLogicalOr(ctx.logicalOr());
        if (ctx.assignment() == null) return leftType;
        YType rightType = resolveAssignment(ctx.assignment());
        if (!YTypeCompatibility.isAssignable(leftType, rightType)) {
            errors.report("Cannot assign value of type '" + rightType + "' to variable of type '" + leftType + "'", ctx);
        }
        return leftType;
    }

    private YType resolveLogicalOr(LogicalOrContext ctx) {
        List<LogicalAndContext> operands = ctx.logicalAnd();
        YType result = resolveLogicalAnd(operands.get(0));
        for (int i = 1; i < operands.size(); i++) {
            YType next = resolveLogicalAnd(operands.get(i));
            if (!YTypeCompatibility.isValidLogicalOperand(result)) errors.report("Operator '||' requires boolean operands, found '" + result + "'", ctx);
            if (!YTypeCompatibility.isValidLogicalOperand(next)) errors.report("Operator '||' requires boolean operands, found '" + next + "'", ctx);
            result = YType.BOOL;
        }
        return result;
    }

    private YType resolveLogicalAnd(LogicalAndContext ctx) {
        List<EqualityContext> operands = ctx.equality();
        YType result = resolveEquality(operands.get(0));
        for (int i = 1; i < operands.size(); i++) {
            YType next = resolveEquality(operands.get(i));
            if (!YTypeCompatibility.isValidLogicalOperand(result)) errors.report("Operator '&&' requires boolean operands, found '" + result + "'", ctx);
            if (!YTypeCompatibility.isValidLogicalOperand(next)) errors.report("Operator '&&' requires boolean operands, found '" + next + "'", ctx);
            result = YType.BOOL;
        }
        return result;
    }

    private YType resolveEquality(EqualityContext ctx) {
        List<RelationalContext> operands = ctx.relational();
        YType result = resolveRelational(operands.get(0));
        for (int i = 1; i < operands.size(); i++) {
            YType next = resolveRelational(operands.get(i));
            if (!YTypeCompatibility.isValidEqualityPair(result, next)) {
                errors.report("Operators '==' / '!=' cannot compare '" + result + "' and '" + next + "'", ctx);
            }
            result = YType.BOOL;
        }
        return result;
    }

    private YType resolveRelational(RelationalContext ctx) {
        List<String> ops = extractOperators(ctx);
        List<AdditiveContext> operands = ctx.additive();
        YType result = resolveAdditive(operands.get(0));
        for (int i = 0; i < ops.size(); i++) {
            YType next = resolveAdditive(operands.get(i + 1));
            if (!YTypeCompatibility.isValidRelationalPair(result, next)) {
                errors.report("Operator '" + ops.get(i) + "' requires numeric operands, found '" + result + "' and '" + next + "'", ctx);
            }
            result = YType.BOOL;
        }
        return result;
    }

    private YType resolveAdditive(AdditiveContext ctx) {
        List<String> ops = extractOperators(ctx);
        List<MultiplicativeContext> operands = ctx.multiplicative();
        YType result = resolveMultiplicative(operands.get(0));
        for (int i = 0; i < ops.size(); i++) {
            YType next = resolveMultiplicative(operands.get(i + 1));
            result = ops.get(i).equals("+") ? checkPlusOperands(result, next, ctx) : checkArithmeticOnly(result, next, ops.get(i), ctx);
        }
        return result;
    }

    private YType resolveMultiplicative(MultiplicativeContext ctx) {
        List<String> ops = extractOperators(ctx);
        List<UnaryContext> operands = ctx.unary();
        YType result = resolveUnary(operands.get(0));
        for (int i = 0; i < ops.size(); i++) {
            YType next = resolveUnary(operands.get(i + 1));
            result = checkArithmeticOnly(result, next, ops.get(i), ctx);
        }
        return result;
    }

    private YType resolveUnary(UnaryContext ctx) {
        if (ctx.postfix() != null) return resolvePostfix(ctx.postfix());
        YType operand = resolveUnary(ctx.unary());
        if (ctx.NOT() != null) {
            if (!YTypeCompatibility.isValidLogicalOperand(operand)) {
                errors.report("Operator '!' requires a boolean operand, found '" + operand + "'", ctx);
            }
            return YType.BOOL;
        }
        if (!operand.isUnknown() && !operand.isNumeric()) {
            errors.report("Unary operator '" + ctx.getChild(0).getText() + "' requires a numeric operand, found '" + operand + "'", ctx);
            return YType.UNKNOWN;
        }
        return operand;
    }

    private YType resolvePostfix(PostfixContext ctx) {
        PrimaryContext primary = ctx.primary();
        List<PostfixOpContext> ops = ctx.postfixOp();

        boolean bareIdentifierCall = primary.IDENTIFIER() != null && !ops.isEmpty() && ops.get(0).LPAREN() != null;
        if (bareIdentifierCall) {
            // foo(args): calls a function from the global function table
            String functionName = primary.IDENTIFIER().getText();
            List<YType> argTypes = resolveArguments(ops.get(0).argumentList());
            YType callType = resolveFunctionCall(functionName, argTypes, ops.get(0));
            return continuePostfixChain(callType, ops, 1);
        }

        YType currentType = resolvePrimary(primary);
        return continuePostfixChain(currentType, ops, 0);
    }

    private YType continuePostfixChain(YType startType, List<PostfixOpContext> ops, int startIndex) {
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
                YType indexType = resolve(op.expression());
                if (!indexType.isUnknown() && indexType.getKind() != YType.Kind.ENTERO) {
                    errors.report("Array index must be of type entero, found '" + indexType + "'", op);
                }
                if (currentType.isArray()) {
                    currentType = currentType.asArray(-1);
                } else if (!currentType.isUnknown()) {
                    errors.report("Cannot index a value of type '" + currentType + "'; it is not an array", op);
                    currentType = YType.UNKNOWN;
                }
            } else {
                // INC or DEC postfix operator
                if (!currentType.isUnknown() && !currentType.isNumeric()) {
                    errors.report("Postfix operator '" + op.getText() + "' requires a numeric operand, found '" + currentType + "'", op);
                }
            }
        }
        return currentType;
    }

    private YType resolvePrimary(PrimaryContext ctx) {
        if (ctx.INT_LITERAL() != null) return YType.ENTERO;
        if (ctx.FLOAT_LITERAL() != null) return YType.FLOTANTE;
        if (ctx.CHAR_LITERAL() != null) return YType.CARACTER;
        if (ctx.STRING_LITERAL() != null) return YType.CADENA;
        if (ctx.VERDADERO() != null || ctx.FALSO() != null) return YType.BOOL;
        if (ctx.IMPRIMIR() != null) {
            resolveArguments(ctx.argumentList());
            return YType.VOID;
        }
        if (ctx.LEER() != null) {
            resolveArguments(ctx.argumentList());
            return YType.CADENA;
        }
        if (ctx.LBRACE() != null) {
            // structure/array literal: contents are visited (so inner errors are still
            // reported) but the literal's own type is intentionally left undetermined, since
            // field/element-type checking for {} literals is postponed to code generation
            for (ExpressionContext e : ctx.expression()) {
                resolve(e);
            }
            return YType.UNKNOWN;
        }
        if (ctx.IDENTIFIER() != null) {
            return resolveIdentifier(ctx);
        }
        if (ctx.LPAREN() != null) {
            return resolve(ctx.expression(0));
        }
        return YType.UNKNOWN;
    }

    private YType resolveIdentifier(PrimaryContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        YVariableSymbol local = currentScope != null ? currentScope.resolve(name) : null;
        if (local != null) return local.getType();
        errors.report("Variable '" + name + "' is not declared", ctx);
        return YType.UNKNOWN;
    }

    private YType resolveFunctionCall(String name, List<YType> argTypes, ParserRuleContext ctx) {
        YFunctionSymbol function = functionRegistry.resolve(name);
        if (function == null) {
            errors.report("Function '" + name + "' is not declared", ctx);
            return YType.UNKNOWN;
        }
        if (function.getArity() != argTypes.size()) {
            errors.report("Function '" + name + "' called with " + argTypes.size() + " argument(s), but declares " + function.getArity(), ctx);
            return YType.UNKNOWN;
        }
        return function.getReturnType();
    }

    private List<YType> resolveArguments(ArgumentListContext ctx) {
        List<YType> types = new ArrayList<>();
        if (ctx == null) return types;
        for (ExpressionContext e : ctx.expression()) {
            types.add(resolve(e));
        }
        return types;
    }

    private YType checkPlusOperands(YType left, YType right, ParserRuleContext ctx) {
        if (left.isUnknown() || right.isUnknown()) return YType.UNKNOWN;
        if (YTypeCompatibility.isStringConcatenation(left, right)) return YType.CADENA;
        if (left.isNumeric() && right.isNumeric()) return YTypeCompatibility.arithmeticResult(left, right);
        errors.report("Operator '+' requires two numeric operands or at least one cadena operand, found '" + left + "' and '" + right + "'", ctx);
        return YType.UNKNOWN;
    }

    private YType checkArithmeticOnly(YType left, YType right, String opSymbol, ParserRuleContext ctx) {
        if (left.isUnknown() || right.isUnknown()) return YType.UNKNOWN;
        if (!(left.isNumeric() && right.isNumeric())) {
            errors.report("Operator '" + opSymbol + "' requires numeric operands, found '" + left + "' and '" + right + "'", ctx);
            return YType.UNKNOWN;
        }
        return YTypeCompatibility.arithmeticResult(left, right);
    }

    private List<String> extractOperators(ParserRuleContext ctx) {
        List<String> ops = new ArrayList<>();
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            ops.add(ctx.getChild(i).getText());
        }
        return ops;
    }
}