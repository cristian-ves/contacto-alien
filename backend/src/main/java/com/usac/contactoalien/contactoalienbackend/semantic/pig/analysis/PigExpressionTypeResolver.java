package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigTypeCompatibility;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ConstructorSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.MethodSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.VariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YFunctionSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YVariableSymbol;
import org.antlr.v4.runtime.tree.ParseTree;
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

    public PigExpressionTypeResolver(PigImportResult importResult, PigSymbolTable symbolTable, PigErrorReporter reporter) {
        this.importResult = importResult;
        this.symbolTable = symbolTable;
        this.reporter = reporter;
    }

    public PigType resolve(PigParser.ExpressionContext ctx) {
        return resolveAssignmentExpr(ctx.assignmentExpr());
    }

    private PigType resolveAssignmentExpr(PigParser.AssignmentExprContext ctx) {
        if (ctx.ASSIGN() != null) {
            // Embedded assignment as an expression (e.g. 'a = b = c') is out of scope for this
            // prompt: only the top-level assignmentStatement validates a target's type. The
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
                if (!PigTypeCompatibility.isValidEqualityPair(result, right)) {
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
                    result = PigType.TEXTUM;
                } else if (result.isNumeric() && right.isNumeric()) {
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
            if (child instanceof TerminalNode terminal && (isStar(terminal) || isDiv(terminal))) {
                PigType right = resolveUnaryExpr(operands.get(operandIndex));
                if (result.isNumeric() && right.isNumeric()) {
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
        if (ctx.NOT() != null) {
            PigType operand = resolveUnaryExpr(ctx.unaryExpr());
            if (!PigTypeCompatibility.isValidLogicalOperand(operand)) {
                reporter.report("operator '!' requires a bool operand, found " + operand, ctx);
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

    // postfixExpression: identifiers, free function calls, method calls, field access, array
    // indexing and object instantiation are all resolved here in a single left-to-right pass
    // over the postfix operator chain.
    public PigType resolvePostfixExpression(PigParser.PostfixExpressionContext ctx) {
        PigParser.PrimaryExpressionContext primary = ctx.primaryExpression();
        List<PigParser.PostfixOpContext> ops = ctx.postfixOp();

        PigType current;
        int nextOpIndex = 0;

        boolean isPlainIdentifier = primary.IDENTIFIER() != null && primary.NOVUS() == null;

        if (isPlainIdentifier) {
            String name = primary.IDENTIFIER().getText();
            boolean isFreeCall = !ops.isEmpty() && ops.get(0).LPAREN() != null;
            if (isFreeCall) {
                current = resolveFreeFunctionCall(name, ops.get(0), ctx);
                nextOpIndex = 1;
            } else {
                PigVariableSymbol variable = symbolTable.resolve(name);
                if (variable == null) {
                    reporter.report("variable '" + name + "' is not declared", ctx);
                    current = PigType.UNKNOWN;
                } else {
                    current = variable.getType();
                }
            }
        } else {
            current = resolvePrimaryNonIdentifier(primary, ctx);
        }

        for (int i = nextOpIndex; i < ops.size(); i++) {
            PigParser.PostfixOpContext op = ops.get(i);
            if (op.DOT() != null) {
                String memberName = op.IDENTIFIER().getText();
                boolean isMethodCall = i + 1 < ops.size() && ops.get(i + 1).LPAREN() != null;
                if (isMethodCall) {
                    current = resolveMethodCall(current, memberName, ops.get(i + 1), ctx);
                    i++;
                } else {
                    current = resolveFieldAccess(current, memberName, ctx);
                }
            } else if (op.LBRACK() != null) {
                resolve(op.expression());
                if (current.getKind() != PigType.Kind.ARRAY) {
                    reporter.report("cannot index a value of type " + current + ", it is not a series", ctx);
                    current = PigType.UNKNOWN;
                } else {
                    current = current.getElementType();
                }
            } else if (op.LPAREN() != null) {
                for (PigParser.ExpressionContext arg : argumentsOf(op)) {
                    resolve(arg);
                }
                reporter.report("cannot call a value of type " + current + ", it is not a function or a method", ctx);
                current = PigType.UNKNOWN;
            }
        }

        return current;
    }

    private PigType resolveFreeFunctionCall(String name, PigParser.PostfixOpContext callOp, PigParser.PostfixExpressionContext ctx) {
        YFunctionSymbol function = importResult.getYSymbols().resolveFunction(name);
        List<PigParser.ExpressionContext> args = argumentsOf(callOp);
        for (PigParser.ExpressionContext arg : args) {
            resolve(arg);
        }
        if (function == null) {
            reporter.report("function '" + name + "' is not declared", ctx);
            return PigType.UNKNOWN;
        }
        if (function.getArity() != args.size()) {
            reporter.report("function '" + name + "' expects " + function.getArity() + " argument(s) but got " + args.size(), ctx);
            return PigType.UNKNOWN;
        }
        return PigType.fromY(function.getReturnType());
    }

    private PigType resolveMethodCall(PigType receiver, String methodName, PigParser.PostfixOpContext callOp, PigParser.PostfixExpressionContext ctx) {
        List<PigParser.ExpressionContext> args = argumentsOf(callOp);
        for (PigParser.ExpressionContext arg : args) {
            resolve(arg);
        }
        if (receiver.isUnknown()) return PigType.UNKNOWN;
        if (receiver.getKind() != PigType.Kind.OBJECT) {
            reporter.report("cannot call method '" + methodName + "' on a value of type " + receiver + ", it is not an object", ctx);
            return PigType.UNKNOWN;
        }
        ClassSymbolTable classTable = importResult.getClassRegistry().resolve(receiver.getClassName());
        if (classTable == null) {
            reporter.report("class '" + receiver.getClassName() + "' is not known", ctx);
            return PigType.UNKNOWN;
        }
        List<MethodSymbol> overloads = classTable.resolveMethods(methodName);
        if (overloads.isEmpty()) {
            reporter.report("method '" + methodName + "' is not declared in class '" + receiver.getClassName() + "'", ctx);
            return PigType.UNKNOWN;
        }
        for (MethodSymbol overload : overloads) {
            if (overload.getArity() == args.size()) {
                return PigType.fromZetariano(overload.getReturnType());
            }
        }
        reporter.report("method '" + methodName + "' in class '" + receiver.getClassName() + "' does not accept " + args.size() + " argument(s)", ctx);
        return PigType.UNKNOWN;
    }

    private PigType resolveFieldAccess(PigType receiver, String fieldName, PigParser.PostfixExpressionContext ctx) {
        if (receiver.isUnknown()) return PigType.UNKNOWN;
        if (receiver.getKind() == PigType.Kind.OBJECT) {
            ClassSymbolTable classTable = importResult.getClassRegistry().resolve(receiver.getClassName());
            VariableSymbol field = classTable == null ? null : classTable.resolveField(fieldName);
            if (field == null) {
                reporter.report("field '" + fieldName + "' is not declared in class '" + receiver.getClassName() + "'", ctx);
                return PigType.UNKNOWN;
            }
            return PigType.fromZetariano(field.getType());
        }
        if (receiver.getKind() == PigType.Kind.STRUCT) {
            YStructSymbol struct = importResult.getYSymbols().resolveStruct(receiver.getStructName());
            YVariableSymbol field = struct == null ? null : struct.resolveField(fieldName);
            if (field == null) {
                reporter.report("field '" + fieldName + "' is not declared in structure '" + receiver.getStructName() + "'", ctx);
                return PigType.UNKNOWN;
            }
            return PigType.fromY(field.getType());
        }
        reporter.report("cannot access field '" + fieldName + "' on a value of type " + receiver + ", it is not an object or a structure", ctx);
        return PigType.UNKNOWN;
    }

    private PigType resolvePrimaryNonIdentifier(PigParser.PrimaryExpressionContext primary, PigParser.PostfixExpressionContext ctx) {
        if (primary.NOVUS() != null) {
            String className = primary.IDENTIFIER().getText();
            List<PigParser.ExpressionContext> args = primary.argumentList() == null
                    ? List.of() : primary.argumentList().expression();
            for (PigParser.ExpressionContext arg : args) {
                resolve(arg);
            }
            if (!importResult.getClassRegistry().contains(className)) {
                reporter.report("class '" + className + "' is not declared", ctx);
                return PigType.UNKNOWN;
            }
            ClassSymbolTable classTable = importResult.getClassRegistry().resolve(className);
            List<ConstructorSymbol> constructors = classTable.getConstructors();
            boolean matches = constructors.isEmpty() ? args.isEmpty()
                    : constructors.stream().anyMatch(c -> c.getArity() == args.size());
            if (!matches) {
                reporter.report("constructor of class '" + className + "' does not accept " + args.size() + " argument(s)", ctx);
                return PigType.UNKNOWN;
            }
            return PigType.ofObject(className);
        }
        if (primary.LPAREN() != null) {
            return resolve(primary.expression().get(0));
        }
        if (primary.LBRACE() != null) {
            for (PigParser.ExpressionContext expr : primary.expression()) {
                resolve(expr);
            }
            // A bare literal list has no fixed target type outside a declaration context
            // (basicVarDecl/arrayDecl/structInstanceDecl already validate it against the
            // declared type there); as a standalone expression it is left as UNKNOWN.
            return PigType.UNKNOWN;
        }
        if (primary.INT_LITERAL() != null) return PigType.NUMERUS;
        if (primary.FLOAT_LITERAL() != null) return PigType.DECIMALIS;
        if (primary.CHAR_LITERAL() != null) return PigType.LITTERA;
        if (primary.STRING_LITERAL() != null) return PigType.TEXTUM;
        if (primary.VERUM() != null) return PigType.BOOL;
        if (primary.FALSUS() != null) return PigType.BOOL;
        return PigType.UNKNOWN;
    }

    private static List<PigParser.ExpressionContext> argumentsOf(PigParser.PostfixOpContext callOp) {
        return callOp.argumentList() == null ? List.of() : callOp.argumentList().expression();
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
}