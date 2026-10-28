package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YFunctionSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YVariableSymbol;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.function.Function;

// Types a postfix expression: a primary followed by calls, member access, indexing and ++/--, resolved
// in one left-to-right pass over the operator chain. Nested expressions go back through the callback.
final class PigPostfixResolver {

    private final PigImportResult importResult;
    private final PigSymbolTable symbolTable;
    private final PigErrorReporter reporter;
    private final Function<PigParser.ExpressionContext, PigType> expressions;
    private final PigIncrementResolver incrementResolver;
    private final PigMemberResolver members;

    PigPostfixResolver(PigImportResult importResult, PigSymbolTable symbolTable, PigErrorReporter reporter,
                       Function<PigParser.ExpressionContext, PigType> expressions) {
        this.importResult = importResult;
        this.symbolTable = symbolTable;
        this.reporter = reporter;
        this.expressions = expressions;
        this.incrementResolver = new PigIncrementResolver(reporter);
        this.members = new PigMemberResolver(importResult, reporter);
    }

    // Identifiers, free function calls, method calls, field access, array
    // indexing and object instantiation are all resolved here in a single left-to-right pass
    // over the postfix operator chain.
    PigType resolve(PigParser.PostfixExpressionContext ctx) {
        PigParser.PrimaryExpressionContext primary = ctx.primaryExpression();
        List<PigParser.PostfixOpContext> ops = ctx.postfixOp();

        PigType current;
        int nextOpIndex = 0;
        int declaredLength = -1;

        boolean isPlainIdentifier = primary.IDENTIFIER() != null && primary.NOVUS() == null;

        if (isPlainIdentifier) {
            String name = primary.IDENTIFIER().getText();
            boolean isFreeCall = !ops.isEmpty() && ops.get(0).LPAREN() != null;
            if (isFreeCall) {
                current = resolveFreeFunctionCall(name, ops.get(0), ctx);
                nextOpIndex = 1;
            } else {
                PigVariableSymbol variable = symbolTable.resolve(name);
                declaredLength = variable == null ? -1 : variable.getLength();
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
                new PigCollectionRules(reporter).checkIndex(expressions.apply(op.expression()), op.expression());
                if (i == nextOpIndex) {
                    new PigCollectionRules(reporter).checkConstantIndex(op.expression(), declaredLength);
                }
                if (current.getKind() != PigType.Kind.ARRAY) {
                    reporter.report("cannot index a value of type " + current + ", it is not a series", ctx);
                    current = PigType.UNKNOWN;
                } else {
                    current = current.getElementType();
                }
            } else if (op.LPAREN() != null) {
                for (PigParser.ExpressionContext arg : argumentsOf(op)) {
                    expressions.apply(arg);
                }
                reporter.report("cannot call a value of type " + current + ", it is not a function or a method", ctx);
                current = PigType.UNKNOWN;
            } else if (op.INC() != null || op.DEC() != null) {
                current = incrementResolver.resolve(op, current, ctx);
            }
        }

        return current;
    }

    private PigType resolveFreeFunctionCall(String name, PigParser.PostfixOpContext callOp, PigParser.PostfixExpressionContext ctx) {
        YFunctionSymbol function = importResult.getYSymbols().resolveFunction(name);
        List<PigParser.ExpressionContext> args = argumentsOf(callOp);
        for (PigParser.ExpressionContext arg : args) {
            expressions.apply(arg);
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
        List<PigType> argumentTypes = resolveAll(argumentsOf(callOp));
        if (receiver.isUnknown()) return PigType.UNKNOWN;
        if (receiver.getKind() != PigType.Kind.OBJECT) {
            reporter.report("cannot call method '" + methodName + "' on a value of type " + receiver + ", it is not an object", ctx);
            return PigType.UNKNOWN;
        }
        return members.call(receiver.getClassName(), methodName, argumentTypes, ctx);
    }

    private List<PigType> resolveAll(List<PigParser.ExpressionContext> arguments) {
        return arguments.stream().map(expressions).toList();
    }

    // `novus Name(args)` in a declaration or an expression.
    public PigType resolveNew(String className, List<PigParser.ExpressionContext> arguments, ParserRuleContext at) {
        return members.construct(className, resolveAll(arguments), at);
    }

    private PigType resolveFieldAccess(PigType receiver, String fieldName, PigParser.PostfixExpressionContext ctx) {
        if (receiver.isUnknown()) return PigType.UNKNOWN;
        if (receiver.getKind() == PigType.Kind.OBJECT) {
            return members.field(receiver.getClassName(), fieldName, ctx);
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
            List<PigParser.ExpressionContext> args = primary.argumentList() == null
                    ? List.of() : primary.argumentList().expression();
            return resolveNew(primary.IDENTIFIER().getText(), args, ctx);
        }
        if (primary.NULL() != null) return PigType.NULL;
        if (primary.LPAREN() != null) {
            return expressions.apply(primary.expression().get(0));
        }
        if (primary.LBRACE() != null) {
            for (PigParser.ExpressionContext expr : primary.expression()) {
                expressions.apply(expr);
            }
            // A bare literal list has no fixed target type outside a declaration context
            // (basicVarDecl/arrayDecl/structInstanceDecl already validate it against the
            // declared type there); as a standalone expression it is left as UNKNOWN.
            return PigType.UNKNOWN;
        }
        if (primary.INT_LITERAL() != null) {
            String text = primary.INT_LITERAL().getText();
            if (!IntegerLiterals.fits(text)) {
                reporter.report(IntegerLiterals.outOfRange(text), primary);
            }
            return PigType.NUMERUS;
        }
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
}
