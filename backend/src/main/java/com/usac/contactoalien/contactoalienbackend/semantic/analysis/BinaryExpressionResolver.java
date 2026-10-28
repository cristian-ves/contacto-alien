package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.*;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.types.TypeCompatibility;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// Types the binary operator levels of an expression (|| down to * and /); the operands of the lowest
// level are unary expressions, resolved by the callback.
final class BinaryExpressionResolver {

    private final CompilationUnitRegistry registry;
    private final ErrorReporter errors;
    private final Function<UnaryExpressionContext, ZType> unary;

    BinaryExpressionResolver(CompilationUnitRegistry registry, ErrorReporter errors,
                             Function<UnaryExpressionContext, ZType> unary) {
        this.registry = registry;
        this.errors = errors;
        this.unary = unary;
    }

    ZType logicalOr(LogicalOrExpressionContext ctx) {
        List<LogicalAndExpressionContext> operands = ctx.logicalAndExpression();
        ZType result = resolveLogicalAnd(operands.get(0));
        for (int i = 1; i < operands.size(); i++) {
            ZType next = resolveLogicalAnd(operands.get(i));
            if (!TypeCompatibility.isValidLogicalOperand(result)) {
                errors.report("Operator '||' requires boolean operands, found '" + result + "'", ctx);
            }
            if (!TypeCompatibility.isValidLogicalOperand(next)) {
                errors.report("Operator '||' requires boolean operands, found '" + next + "'", ctx);
            }
            result = ZType.BOOLEAN;
        }
        return result;
    }

    private ZType resolveLogicalAnd(LogicalAndExpressionContext ctx) {
        List<EqualityExpressionContext> operands = ctx.equalityExpression();
        ZType result = resolveEquality(operands.get(0));
        for (int i = 1; i < operands.size(); i++) {
            ZType next = resolveEquality(operands.get(i));
            if (!TypeCompatibility.isValidLogicalOperand(result)) {
                errors.report("Operator '&&' requires boolean operands, found '" + result + "'", ctx);
            }
            if (!TypeCompatibility.isValidLogicalOperand(next)) {
                errors.report("Operator '&&' requires boolean operands, found '" + next + "'", ctx);
            }
            result = ZType.BOOLEAN;
        }
        return result;
    }

    private ZType resolveEquality(EqualityExpressionContext ctx) {
        List<RelationalExpressionContext> operands = ctx.relationalExpression();
        ZType result = resolveRelational(operands.get(0));
        for (int i = 1; i < operands.size(); i++) {
            ZType next = resolveRelational(operands.get(i));
            if (!TypeCompatibility.isValidEqualityPair(result, next, registry)) {
                errors.report("Operators '==' / '!=' cannot compare '" + result + "' and '" + next + "'", ctx);
            }
            result = ZType.BOOLEAN;
        }
        return result;
    }

    private ZType resolveRelational(RelationalExpressionContext ctx) {
        List<String> ops = extractOperators(ctx);
        List<AdditiveExpressionContext> operands = ctx.additiveExpression();
        ZType result = resolveAdditive(operands.get(0));
        for (int i = 0; i < ops.size(); i++) {
            ZType next = resolveAdditive(operands.get(i + 1));
            if (!TypeCompatibility.isValidRelationalPair(result, next)) {
                errors.report("Operator '" + ops.get(i) + "' requires numeric operands, found '" + result + "' and '" + next + "'", ctx);
            }
            result = ZType.BOOLEAN;
        }
        return result;
    }

    private ZType resolveAdditive(AdditiveExpressionContext ctx) {
        List<String> ops = extractOperators(ctx);
        List<MultiplicativeExpressionContext> operands = ctx.multiplicativeExpression();
        ZType result = resolveMultiplicative(operands.get(0));
        for (int i = 0; i < ops.size(); i++) {
            ZType next = resolveMultiplicative(operands.get(i + 1));
            result = ops.get(i).equals("+")
                    ? plus(result, next, ctx)
                    : arithmetic(result, next, ops.get(i), ctx);
        }
        return result;
    }

    private ZType resolveMultiplicative(MultiplicativeExpressionContext ctx) {
        List<String> ops = extractOperators(ctx);
        List<UnaryExpressionContext> operands = ctx.unaryExpression();
        ZType result = unary.apply(operands.get(0));
        for (int i = 0; i < ops.size(); i++) {
            ZType next = unary.apply(operands.get(i + 1));
            result = arithmetic(result, next, ops.get(i), ctx);
        }
        return result;
    }

    ZType plus(ZType left, ZType right, ParserRuleContext ctx) {
        if (left.isUnknown() || right.isUnknown()) return ZType.UNKNOWN;
        if (TypeCompatibility.isStringConcatenation(left, right)) {
            ZType other = left.getKind() == ZType.Kind.STRING && !left.isArray() ? right : left;
            if (other.isArray() || other.getKind() == ZType.Kind.CLASS || other.getKind() == ZType.Kind.VOID) {
                errors.report("Cannot concatenate a value of type '" + other + "' to a String; use its fields or elements one by one", ctx);
            }
            return ZType.STRING;
        }
        if (left.isNumeric() && right.isNumeric()) return TypeCompatibility.arithmeticResult(left, right);
        errors.report("Operator '+' requires two numeric operands or at least one String operand, found '" + left + "' and '" + right + "'", ctx);
        return ZType.UNKNOWN;
    }

    private ZType arithmetic(ZType left, ZType right, String opSymbol, ParserRuleContext ctx) {
        if (left.isUnknown() || right.isUnknown()) return ZType.UNKNOWN;
        if (!(left.isNumeric() && right.isNumeric())) {
            errors.report("Operator '" + opSymbol + "' requires numeric operands, found '" + left + "' and '" + right + "'", ctx);
            return ZType.UNKNOWN;
        }
        return TypeCompatibility.arithmeticResult(left, right);
    }

    private List<String> extractOperators(ParserRuleContext ctx) {
        List<String> ops = new ArrayList<>();
        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            ops.add(ctx.getChild(i).getText());
        }
        return ops;
    }
}
