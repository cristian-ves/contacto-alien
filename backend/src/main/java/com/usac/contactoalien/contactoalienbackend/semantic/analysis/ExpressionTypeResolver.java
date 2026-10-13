package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.*;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ConstructorSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.MethodSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.Scope;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.VariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.types.TypeCompatibility;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

// Computes the ZType of any expression subtree, reporting every semantic error it finds along
// the way (undeclared variables, wrong argument counts, incompatible operators, invalid member
// access) while returning ZType.UNKNOWN to suppress further cascading errors on that branch.
// Member access on a class-typed value (fields, methods, 'this') is resolved against whichever
// ClassSymbolTable matches that class: the class under analysis, or another class looked up in
// the CompilationUnitRegistry.
public final class ExpressionTypeResolver {

    private final ClassSymbolTable classSymbolTable;
    private final CompilationUnitRegistry registry;
    private final ErrorReporter errors;
    private Scope currentScope;

    public ExpressionTypeResolver(ClassSymbolTable classSymbolTable, CompilationUnitRegistry registry, ErrorReporter errors) {
        this.classSymbolTable = classSymbolTable;
        this.registry = registry;
        this.errors = errors;
    }

    public void setScope(Scope scope) {
        this.currentScope = scope;
    }

    public ZType resolve(ExpressionContext ctx) {
        return resolveAssignment(ctx.assignmentExpression());
    }

    private ZType resolveAssignment(AssignmentExpressionContext ctx) {
        ZType leftType = resolveTernary(ctx.ternaryExpression());
        if (ctx.assignmentExpression() == null) {
            return leftType;
        }
        ZType rightType = resolveAssignment(ctx.assignmentExpression());
        if (ctx.PLUS_ASSIGN() != null) {
            // '+=' behaves like '+' for operand validity (numeric or string concatenation)
            checkPlusOperands(leftType, rightType, ctx);
        } else if (ctx.MINUS_ASSIGN() != null || ctx.MUL_ASSIGN() != null) {
            // '-=' and '*=' require numeric operands on both sides, same as their binary counterparts
            if (!leftType.isUnknown() && !rightType.isUnknown() && !(leftType.isNumeric() && rightType.isNumeric())) {
                errors.report("Operator '" + ctx.getChild(1).getText() + "' requires numeric operands, found '" + leftType + "' and '" + rightType + "'", ctx);
            }
        } else {
            // plain '='
            if (!TypeCompatibility.isAssignable(leftType, rightType)) {
                errors.report("Cannot assign value of type '" + rightType + "' to variable of type '" + leftType + "'", ctx);
            }
        }
        return leftType;
    }

    private ZType resolveTernary(TernaryExpressionContext ctx) {
        ZType condition = resolveLogicalOr(ctx.logicalOrExpression());
        if (ctx.QUESTION() == null) {
            return condition;
        }
        if (!TypeCompatibility.isValidLogicalOperand(condition)) {
            errors.report("Ternary condition must be boolean, found '" + condition + "'", ctx.logicalOrExpression());
        }
        List<ExpressionContext> branches = ctx.expression();
        ZType thenType = resolve(branches.get(0));
        ZType elseType = resolve(branches.get(1));
        if (thenType.isUnknown() || elseType.isUnknown()) return ZType.UNKNOWN;
        if (TypeCompatibility.isAssignable(thenType, elseType)) return thenType;
        if (TypeCompatibility.isAssignable(elseType, thenType)) return elseType;
        errors.report("Ternary branches have incompatible types '" + thenType + "' and '" + elseType + "'", ctx);
        return ZType.UNKNOWN;
    }

    private ZType resolveLogicalOr(LogicalOrExpressionContext ctx) {
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
            if (!TypeCompatibility.isValidEqualityPair(result, next)) {
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
                    ? checkPlusOperands(result, next, ctx)
                    : checkArithmeticOnly(result, next, ops.get(i), ctx);
        }
        return result;
    }

    private ZType resolveMultiplicative(MultiplicativeExpressionContext ctx) {
        List<String> ops = extractOperators(ctx);
        List<UnaryExpressionContext> operands = ctx.unaryExpression();
        ZType result = resolveUnary(operands.get(0));
        for (int i = 0; i < ops.size(); i++) {
            ZType next = resolveUnary(operands.get(i + 1));
            result = checkArithmeticOnly(result, next, ops.get(i), ctx);
        }
        return result;
    }

    private ZType resolveUnary(UnaryExpressionContext ctx) {
        if (ctx.postfixExpression() != null) {
            return resolvePostfix(ctx.postfixExpression());
        }
        ZType operand = resolveUnary(ctx.unaryExpression());
        if (ctx.NOT() != null) {
            if (!TypeCompatibility.isValidLogicalOperand(operand)) {
                errors.report("Operator '!' requires a boolean operand, found '" + operand + "'", ctx);
            }
            return ZType.BOOLEAN;
        }
        // unary +, -, ++, -- all require a numeric operand and preserve its type
        if (!operand.isUnknown() && !operand.isNumeric()) {
            errors.report("Unary operator '" + ctx.getChild(0).getText() + "' requires a numeric operand, found '" + operand + "'", ctx);
            return ZType.UNKNOWN;
        }
        return operand;
    }

    private ZType resolvePostfix(PostfixExpressionContext ctx) {
        PrimaryExpressionContext primary = ctx.primaryExpression();
        List<PostfixOpContext> ops = ctx.postfixOp();

        boolean bareIdentifierCall = primary.IDENTIFIER() != null && !ops.isEmpty() && ops.get(0).LPAREN() != null;
        if (bareIdentifierCall) {
            // foo(args) with no receiver: calls a method of the class being analyzed itself (supports recursion)
            String methodName = primary.IDENTIFIER().getText();
            List<ZType> argTypes = resolveArguments(ops.get(0).argumentList());
            ZType callType = resolveMethodCall(methodName, argTypes, ops.get(0), classSymbolTable);
            return continuePostfixChain(callType, ops, 1);
        }

        ZType currentType = resolvePrimary(primary);
        return continuePostfixChain(currentType, ops, 0);
    }

    // Returns the ClassSymbolTable that declares the given class-kind ZType: the class under
    // analysis if it matches, otherwise the corresponding table from the registry.
    private ClassSymbolTable tableForClassType(ZType classType) {
        if (classType.getClassName().equals(classSymbolTable.getClassName())) {
            return classSymbolTable;
        }
        return registry.resolve(classType.getClassName());
    }

    private ZType continuePostfixChain(ZType startType, List<PostfixOpContext> ops, int startIndex) {
        ZType currentType = startType;
        int i = startIndex;
        while (i < ops.size()) {
            PostfixOpContext op = ops.get(i);
            if (op.DOT() != null) {
                String memberName = op.IDENTIFIER().getText();
                boolean nextIsCall = (i + 1 < ops.size()) && ops.get(i + 1).LPAREN() != null;
                if (currentType.isUnknown()) {
                    currentType = ZType.UNKNOWN;
                } else if (currentType.getKind() != ZType.Kind.CLASS) {
                    errors.report("Cannot access '" + memberName + "' on a value of type '" + currentType + "', which is not an object of a known class", op);
                    currentType = ZType.UNKNOWN;
                } else {
                    ClassSymbolTable targetTable = tableForClassType(currentType);
                    if (nextIsCall) {
                        List<ZType> argTypes = resolveArguments(ops.get(i + 1).argumentList());
                        currentType = resolveMethodCall(memberName, argTypes, op, targetTable);
                        i++; // the following LPAREN op belongs to this same call, consume it too
                    } else {
                        VariableSymbol field = targetTable.resolveField(memberName);
                        if (field == null) {
                            errors.report("Class '" + targetTable.getClassName() + "' has no field named '" + memberName + "'", op);
                            currentType = ZType.UNKNOWN;
                        } else {
                            currentType = field.getType();
                        }
                    }
                }
            } else if (op.LPAREN() != null) {
                // a call not immediately preceded by an identifier/DOT: this language has no first-class functions
                if (!currentType.isUnknown()) {
                    errors.report("Expression is not callable", op);
                }
                currentType = ZType.UNKNOWN;
            } else if (op.LBRACKET() != null) {
                ZType indexType = resolve(op.expression());
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
                if (!currentType.isUnknown() && !currentType.isNumeric()) {
                    errors.report("Postfix operator '" + op.getText() + "' requires a numeric operand, found '" + currentType + "'", op);
                }
            }
            i++;
        }
        return currentType;
    }

    private ZType resolvePrimary(PrimaryExpressionContext ctx) {
        if (ctx.INT_LITERAL() != null) return ZType.INT;
        if (ctx.DOUBLE_LITERAL() != null) return ZType.DOUBLE;
        if (ctx.CHAR_LITERAL() != null) return ZType.CHAR;
        if (ctx.STRING_LITERAL() != null) return ZType.STRING;
        if (ctx.TRUE() != null || ctx.FALSE() != null) return ZType.BOOLEAN;
        if (ctx.NULL() != null) return ZType.NULL;
        if (ctx.THIS() != null) return classSymbolTable.getOwnType();
        if (ctx.READLN() != null) return ZType.STRING;
        if (ctx.NEW() != null) return resolveObjectCreation(ctx);
        if (ctx.IDENTIFIER() != null) return resolveIdentifier(ctx);
        if (ctx.expression() != null) return resolve(ctx.expression());
        return ZType.UNKNOWN;
    }

    private ZType resolveObjectCreation(PrimaryExpressionContext ctx) {
        String className = ctx.IDENTIFIER().getText();
        List<ZType> argTypes = resolveArguments(ctx.argumentList());
        ClassSymbolTable targetTable = className.equals(classSymbolTable.getClassName())
                ? classSymbolTable
                : registry.resolve(className);
        if (targetTable == null) {
            errors.report("Unknown class '" + className + "'; no class named '" + className + "' is declared or registered", ctx);
            return ZType.UNKNOWN;
        }
        List<ConstructorSymbol> constructors = targetTable.getConstructors();
        if (constructors.isEmpty()) {
            // no explicit constructor declared: assume an implicit no-argument constructor, as in Java
            if (!argTypes.isEmpty()) {
                errors.report("Class '" + className + "' has no declared constructor; only no-argument construction is allowed", ctx);
            }
        } else {
            boolean matches = constructors.stream().anyMatch(c -> c.getArity() == argTypes.size());
            if (!matches) {
                errors.report("No constructor of '" + className + "' accepts " + argTypes.size() + " argument(s)", ctx);
            }
        }
        return ZType.ofClass(className);
    }

    private ZType resolveIdentifier(PrimaryExpressionContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        VariableSymbol local = currentScope != null ? currentScope.resolve(name) : null;
        if (local != null) return local.getType();
        VariableSymbol field = classSymbolTable.resolveField(name);
        if (field != null) return field.getType();
        errors.report("Variable '" + name + "' is not declared", ctx);
        return ZType.UNKNOWN;
    }

    private ZType resolveMethodCall(String name, List<ZType> argTypes, ParserRuleContext ctx, ClassSymbolTable table) {
        List<MethodSymbol> candidates = table.resolveMethods(name);
        if (candidates.isEmpty()) {
            errors.report("Method '" + name + "' is not declared in class '" + table.getClassName() + "'", ctx);
            return ZType.UNKNOWN;
        }
        for (MethodSymbol candidate : candidates) {
            if (candidate.getArity() == argTypes.size()) {
                return candidate.getReturnType();
            }
        }
        errors.report("Method '" + name + "' called with " + argTypes.size() + " argument(s), but no overload with that many parameters exists", ctx);
        return ZType.UNKNOWN;
    }

    private List<ZType> resolveArguments(ArgumentListContext ctx) {
        List<ZType> types = new ArrayList<>();
        if (ctx == null) return types;
        for (ExpressionContext e : ctx.expression()) {
            types.add(resolve(e));
        }
        return types;
    }

    private ZType checkPlusOperands(ZType left, ZType right, ParserRuleContext ctx) {
        if (left.isUnknown() || right.isUnknown()) return ZType.UNKNOWN;
        if (TypeCompatibility.isStringConcatenation(left, right)) return ZType.STRING;
        if (left.isNumeric() && right.isNumeric()) return TypeCompatibility.arithmeticResult(left, right);
        errors.report("Operator '+' requires two numeric operands or at least one String operand, found '" + left + "' and '" + right + "'", ctx);
        return ZType.UNKNOWN;
    }

    private ZType checkArithmeticOnly(ZType left, ZType right, String opSymbol, ParserRuleContext ctx) {
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