package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
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
    private final YCollectionRules collections;
    private final YPostfixChain postfixChain;
    private YScope currentScope;

    public YExpressionTypeResolver(YStructRegistry structRegistry, YFunctionRegistry functionRegistry, YErrorReporter errors) {
        this.structRegistry = structRegistry;
        this.functionRegistry = functionRegistry;
        this.errors = errors;
        this.collections = new YCollectionRules(errors);
        this.postfixChain = new YPostfixChain(structRegistry, errors, this::resolve);
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
        PostfixContext target = YPlaceRules.descend(ctx.logicalOr(), PostfixContext.class);
        if (target == null || !YPlaceRules.isPlace(target)) {
            errors.report("The left side of '=' must be a variable, a structure field or an array element", ctx);
        }
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
            result = ops.get(i).equals("+") ? YOperatorRules.plus(errors, result, next, ctx) : YOperatorRules.arithmetic(errors, result, next, ops.get(i), ctx);
        }
        return result;
    }

    private YType resolveMultiplicative(MultiplicativeContext ctx) {
        List<String> ops = extractOperators(ctx);
        List<UnaryContext> operands = ctx.unary();
        YType result = resolveUnary(operands.get(0));
        for (int i = 0; i < ops.size(); i++) {
            YType next = resolveUnary(operands.get(i + 1));
            result = YOperatorRules.arithmetic(errors, result, next, ops.get(i), ctx);
        }
        return result;
    }

    private YType resolveUnary(UnaryContext ctx) {
        if (ctx.postfix() != null) return resolvePostfix(ctx.postfix());
        YType operand = resolveUnary(ctx.unary());
        if (ctx.INC() != null || ctx.DEC() != null) {
            PostfixContext target = YPlaceRules.descend(ctx.unary(), PostfixContext.class);
            if (target == null || !YPlaceRules.isPlace(target)) {
                errors.report("Operand of '" + ctx.getChild(0).getText() + "' must be a variable, a structure field or an array element", ctx);
            }
        }
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
            return postfixChain.continueFrom(callType, primary, ops, 1, -1);
        }

        YType currentType = resolvePrimary(primary);
        if (primary.LEER() != null && !ops.isEmpty()) {
            errors.report("The value read by leer() is not a structure or an array; store it in a variable first", ops.get(0));
            return YType.UNKNOWN;
        }
        YVariableSymbol variable = primary.IDENTIFIER() != null && currentScope != null
                ? currentScope.resolve(primary.IDENTIFIER().getText()) : null;
        return postfixChain.continueFrom(currentType, primary, ops, 0, variable == null ? -1 : variable.getLength());
    }

    private YType resolvePrimary(PrimaryContext ctx) {
        if (ctx.INT_LITERAL() != null) {
            String text = ctx.INT_LITERAL().getText();
            if (!IntegerLiterals.fits(text)) {
                errors.report(IntegerLiterals.outOfRange(text), ctx);
            }
            return YType.ENTERO;
        }
        if (ctx.FLOAT_LITERAL() != null) return YType.FLOTANTE;
        if (ctx.CHAR_LITERAL() != null) return YType.CARACTER;
        if (ctx.STRING_LITERAL() != null) return YType.CADENA;
        if (ctx.VERDADERO() != null || ctx.FALSO() != null) return YType.BOOL;
        if (ctx.IMPRIMIR() != null) {
            resolveArguments(ctx.argumentList()).forEach(printed -> YOperatorRules.requirePrintable(errors, printed, ctx));
            return YType.VOID;
        }
        if (ctx.LEER() != null) {
            if (ctx.argumentList() != null) {
                errors.report("leer() takes no arguments", ctx);
            }
            return YType.UNKNOWN;
        }
        if (ctx.LBRACE() != null) {
            // a list of scalars of one type is an array; anything else may be a structure value list
            // and stays undetermined
            List<YType> values = new ArrayList<>();
            for (ExpressionContext e : ctx.expression()) {
                values.add(resolve(e));
            }
            return collections.listType(values);
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
        collections.checkArguments(function, argTypes, ctx);
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

    private List<String> extractOperators(ParserRuleContext ctx) {
        List<String> ops = new ArrayList<>();
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            ops.add(ctx.getChild(i).getText());
        }
        return ops;
    }
}