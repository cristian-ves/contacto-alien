package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigTypeCompatibility;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

// Resolves the Pig Latin type of any expression node, reporting semantic errors (undeclared
// variables, unknown functions/methods/fields, arity mismatches, invalid operands) as they are
// found, so callers only ever need the final resolved type.
//
// Binary operator chains (multiplicative, additive, relational, equality) can mix distinct
// operator tokens (e.g. STAR/DIV) within a single rule; since the grammar has no alternative
// labels, the auto-generated accessors would separate operator tokens from operand contexts
// into independently-ordered lists, losing which operator applies between which operands. To
// stay correct regardless of that, those four levels walk the raw child sequence and match
// operators by their literal text instead of relying on token-specific accessors.
public final class PigExpressionTypeResolver {

    private final PigImportResult importResult;
    private final PigSymbolTable symbolTable;
    private final PigErrorReporter reporter;
    private final PigPostfixResolver postfix;

    public PigExpressionTypeResolver(PigImportResult importResult, PigSymbolTable symbolTable, PigErrorReporter reporter) {
        this.importResult = importResult;
        this.symbolTable = symbolTable;
        this.reporter = reporter;
        this.postfix = new PigPostfixResolver(importResult, symbolTable, reporter, this::resolve);
    }

    public PigType resolve(PigParser.ExpressionContext ctx) {
        return resolveAssignmentExpr(ctx.assignmentExpr());
    }

    private PigType resolveAssignmentExpr(PigParser.AssignmentExprContext ctx) {
        if (ctx.ASSIGN() != null) {
            // Embedded assignment as an expression (e.g. 'a = b = c') is out of scope for
            // now: only the top-level assignmentStatement validates a target's type. The
            // nested value is still resolved so any error inside it is reported.
            resolveLogicalOrExpr(ctx.logicalOrExpr());
            return resolveAssignmentExpr(ctx.assignmentExpr());
        }
        return resolveLogicalOrExpr(ctx.logicalOrExpr());
    }

    private PigType resolveLogicalOrExpr(PigParser.LogicalOrExprContext ctx) {
        List<PigParser.LogicalAndExprContext> operands = ctx.logicalAndExpr();
        PigType result = resolveLogicalAndExpr(operands.get(0));
        for (int i = 1; i < operands.size(); i++) {
            PigType right = resolveLogicalAndExpr(operands.get(i));
            if (!PigTypeCompatibility.isValidLogicalOperand(result) || !PigTypeCompatibility.isValidLogicalOperand(right)) {
                reporter.report("operator '||' requires bool operands", ctx);
            }
            result = PigType.BOOL;
        }
        return result;
    }

    private PigType resolveLogicalAndExpr(PigParser.LogicalAndExprContext ctx) {
        List<PigParser.EqualityExprContext> operands = ctx.equalityExpr();
        PigType result = resolveEqualityExpr(operands.get(0));
        for (int i = 1; i < operands.size(); i++) {
            PigType right = resolveEqualityExpr(operands.get(i));
            if (!PigTypeCompatibility.isValidLogicalOperand(result) || !PigTypeCompatibility.isValidLogicalOperand(right)) {
                reporter.report("operator '&&' requires bool operands", ctx);
            }
            result = PigType.BOOL;
        }
        return result;
    }

    private PigType resolveEqualityExpr(PigParser.EqualityExprContext ctx) {
        List<PigParser.RelationalExprContext> operands = ctx.relationalExpr();
        PigType result = resolveRelationalExpr(operands.get(0));
        int operandIndex = 1;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode terminal && isEqualityOperator(terminal)) {
                PigType right = resolveRelationalExpr(operands.get(operandIndex));
                if (!PigTypeCompatibility.isValidEqualityPair(result, right, importResult.getClassRegistry())) {
                    reporter.report("operator '" + terminal.getText() + "' cannot compare " + result + " with " + right, ctx);
                }
                result = PigType.BOOL;
                operandIndex++;
            }
        }
        return result;
    }

    private PigType resolveRelationalExpr(PigParser.RelationalExprContext ctx) {
        List<PigParser.AdditiveExprContext> operands = ctx.additiveExpr();
        PigType result = resolveAdditiveExpr(operands.get(0));
        int operandIndex = 1;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode terminal && isRelationalOperator(terminal)) {
                PigType right = resolveAdditiveExpr(operands.get(operandIndex));
                if (!PigTypeCompatibility.isValidRelationalPair(result, right)) {
                    reporter.report("operator '" + terminal.getText() + "' requires numeric operands, found " + result + " and " + right, ctx);
                }
                result = PigType.BOOL;
                operandIndex++;
            }
        }
        return result;
    }

    private PigType resolveAdditiveExpr(PigParser.AdditiveExprContext ctx) {
        List<PigParser.MultiplicativeExprContext> operands = ctx.multiplicativeExpr();
        PigType result = resolveMultiplicativeExpr(operands.get(0));
        int operandIndex = 1;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode terminal && (isPlus(terminal) || isMinus(terminal))) {
                PigType right = resolveMultiplicativeExpr(operands.get(operandIndex));
                if (isPlus(terminal) && PigTypeCompatibility.isStringConcatenation(result, right)) {
                    PigType other = result.getKind() == PigType.Kind.TEXTUM ? right : result;
                    if (other.getKind() == PigType.Kind.ARRAY || other.getKind() == PigType.Kind.OBJECT
                            || other.getKind() == PigType.Kind.VOID) {
                        reporter.report("cannot concatenate a value of type " + other + " to a textum; use its fields or elements one by one", ctx);
                    }
                    result = PigType.TEXTUM;
                } else if (result.isArithmetic() && right.isArithmetic()) {
                    result = PigTypeCompatibility.arithmeticResult(result, right);
                } else if (result.isUnknown() || right.isUnknown()) {
                    result = PigType.UNKNOWN;
                } else {
                    reporter.report("operator '" + terminal.getText() + "' cannot be applied to " + result + " and " + right, ctx);
                    result = PigType.UNKNOWN;
                }
                operandIndex++;
            }
        }
        return result;
    }

    private PigType resolveMultiplicativeExpr(PigParser.MultiplicativeExprContext ctx) {
        List<PigParser.UnaryExprContext> operands = ctx.unaryExpr();
        PigType result = resolveUnaryExpr(operands.get(0));
        int operandIndex = 1;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode terminal && (isStar(terminal) || isDiv(terminal) || isMod(terminal))) {
                PigType right = resolveUnaryExpr(operands.get(operandIndex));
                if (isMod(terminal) && (result.getKind() == PigType.Kind.DECIMALIS || right.getKind() == PigType.Kind.DECIMALIS)) {
                    reporter.report("operator '%' requires integer operands, found " + result + " and " + right, ctx);
                    result = PigType.UNKNOWN;
                } else if (result.isArithmetic() && right.isArithmetic()) {
                    result = PigTypeCompatibility.arithmeticResult(result, right);
                } else if (result.isUnknown() || right.isUnknown()) {
                    result = PigType.UNKNOWN;
                } else {
                    reporter.report("operator '" + terminal.getText() + "' requires numeric operands, found " + result + " and " + right, ctx);
                    result = PigType.UNKNOWN;
                }
                operandIndex++;
            }
        }
        return result;
    }

    private PigType resolveUnaryExpr(PigParser.UnaryExprContext ctx) {
        if (ctx.NOT() != null || ctx.NON() != null) {
            PigType operand = resolveUnaryExpr(ctx.unaryExpr());
            if (!PigTypeCompatibility.isValidLogicalOperand(operand)) {
                reporter.report("operator '" + (ctx.NON() != null ? "non" : "!") + "' requires a bool operand, found " + operand, ctx);
            }
            return PigType.BOOL;
        }
        if (ctx.MINUS() != null || ctx.PLUS() != null) {
            PigType operand = resolveUnaryExpr(ctx.unaryExpr());
            if (operand.isUnknown()) return PigType.UNKNOWN;
            if (!operand.isNumeric()) {
                reporter.report("operator '" + (ctx.MINUS() != null ? "-" : "+") + "' requires a numeric operand, found " + operand, ctx);
                return PigType.UNKNOWN;
            }
            return operand;
        }
        return resolvePostfixExpression(ctx.postfixExpression());
    }

    // Identifiers, calls, member access and indexing are resolved by PigPostfixResolver.
    public PigType resolvePostfixExpression(PigParser.PostfixExpressionContext ctx) {
        return postfix.resolve(ctx);
    }

    // `novus Name(args)` in a declaration or an expression.
    public PigType resolveNew(String className, List<PigParser.ExpressionContext> arguments, ParserRuleContext at) {
        return postfix.resolveNew(className, arguments, at);
    }

    private static boolean isEqualityOperator(TerminalNode node) {
        String text = node.getText();
        return text.equals("==") || text.equals("!=");
    }

    private static boolean isRelationalOperator(TerminalNode node) {
        String text = node.getText();
        return text.equals("<") || text.equals(">") || text.equals("<=") || text.equals(">=");
    }

    private static boolean isPlus(TerminalNode node) { return node.getText().equals("+"); }
    private static boolean isMinus(TerminalNode node) { return node.getText().equals("-"); }
    private static boolean isStar(TerminalNode node) { return node.getText().equals("*"); }
    private static boolean isDiv(TerminalNode node) { return node.getText().equals("/"); }
    private static boolean isMod(TerminalNode node) { return node.getText().equals("%"); }
}