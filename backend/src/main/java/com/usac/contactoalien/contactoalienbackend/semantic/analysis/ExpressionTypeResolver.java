package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
import com.usac.contactoalien.contactoalienbackend.grammar.ReadlnForms;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.*;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
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
    private final MemberResolver members;
    private final BinaryExpressionResolver binary;
    private final PostfixChainResolver chain;
    private Scope currentScope;
    // Set by StatementAnalyzer only while it resolves the first statement of a constructor.
    private boolean superConstructorCallAllowed;

    public ExpressionTypeResolver(ClassSymbolTable classSymbolTable, CompilationUnitRegistry registry, ErrorReporter errors) {
        this.classSymbolTable = classSymbolTable;
        this.registry = registry;
        this.errors = errors;
        this.members = new MemberResolver(classSymbolTable, registry, errors);
        this.binary = new BinaryExpressionResolver(registry, errors, this::resolveUnary);
        this.chain = new PostfixChainResolver(classSymbolTable, members, errors, this::resolve, this::resolveArguments);
    }

    public boolean isAssignable(ZType target, ZType source) {
        return TypeCompatibility.isAssignable(target, source, registry);
    }

    // The statement `super(...)` that opens a constructor body.
    public void resolveSuperConstructorCall(ExpressionContext ctx) {
        superConstructorCallAllowed = true;
        resolve(ctx);
        superConstructorCallAllowed = false;
    }

    public void setScope(Scope scope) {
        this.currentScope = scope;
    }

    public ZType resolve(ExpressionContext ctx) {
        return resolveAssignment(ctx.assignmentExpression());
    }

    // Initializer of a variable of the expected type: a bare readln() is converted to that type.
    public ZType resolve(ExpressionContext ctx, ZType expected) {
        return readsAs(ctx, expected) ? expected : resolve(ctx);
    }

    private static boolean readsAs(ParserRuleContext expression, ZType target) {
        boolean scalar = !target.isArray() && (target.isNumeric() || target.getKind() == ZType.Kind.BOOLEAN);
        return scalar && ReadlnForms.isBareCall(expression);
    }

    private ZType resolveAssignment(AssignmentExpressionContext ctx) {
        ZType leftType = resolveTernary(ctx.ternaryExpression());
        if (ctx.assignmentExpression() == null) {
            return leftType;
        }
        if (chain.endsInArrayLength()) {
            errors.report("Cannot assign to the length of an array", ctx);
        } else if (!ZPlaceRules.isPlace(ctx.ternaryExpression())) {
            errors.report("The left side of '" + ctx.getChild(1).getText() + "' must be a variable, a field or an array element", ctx);
        }
        boolean readsLine = ctx.ASSIGN() != null && readsAs(ctx.assignmentExpression(), leftType);
        ZType rightType = readsLine ? leftType : resolveAssignment(ctx.assignmentExpression());
        if (ctx.PLUS_ASSIGN() != null) {
            // '+=' behaves like '+' for operand validity (numeric or string concatenation)
            ZType result = binary.plus(leftType, rightType, ctx);
            // the sum may be narrowed to a numeric target (as in Java) but text only fits a String target
            if (result.getKind() == ZType.Kind.STRING && !result.isArray() && !leftType.isUnknown()
                    && leftType.getKind() != ZType.Kind.STRING) {
                errors.report("Cannot assign value of type '" + result + "' to variable of type '" + leftType + "'", ctx);
            }
        } else if (ctx.MINUS_ASSIGN() != null || ctx.MUL_ASSIGN() != null) {
            // '-=' and '*=' require numeric operands on both sides, same as their binary counterparts
            if (!leftType.isUnknown() && !rightType.isUnknown() && !(leftType.isNumeric() && rightType.isNumeric())) {
                errors.report("Operator '" + ctx.getChild(1).getText() + "' requires numeric operands, found '" + leftType + "' and '" + rightType + "'", ctx);
            }
        } else {
            // plain '='
            if (!TypeCompatibility.isAssignable(leftType, rightType, registry)) {
                errors.report("Cannot assign value of type '" + rightType + "' to variable of type '" + leftType + "'", ctx);
            }
        }
        return leftType;
    }

    private ZType resolveTernary(TernaryExpressionContext ctx) {
        ZType condition = binary.logicalOr(ctx.logicalOrExpression());
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
        if (TypeCompatibility.isAssignable(thenType, elseType, registry)) return thenType;
        if (TypeCompatibility.isAssignable(elseType, thenType, registry)) return elseType;
        errors.report("Ternary branches have incompatible types '" + thenType + "' and '" + elseType + "'", ctx);
        return ZType.UNKNOWN;
    }

    private ZType resolveUnary(UnaryExpressionContext ctx) {
        if (ctx.postfixExpression() != null) {
            return resolvePostfix(ctx.postfixExpression());
        }
        if (ctx.castType() != null) {
            return resolveCast(ctx);
        }
        ZType operand = resolveUnary(ctx.unaryExpression());
        boolean step = ctx.PLUS_PLUS() != null || ctx.MINUS_MINUS() != null;
        if (step && !ZPlaceRules.isPlace(ctx.unaryExpression())) {
            errors.report("Operand of '" + ctx.getChild(0).getText() + "' must be a variable, a field or an array element", ctx);
        }
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
        // -c and +c promote a char to int; ++c and --c keep it
        boolean sign = ctx.PLUS_PLUS() == null && ctx.MINUS_MINUS() == null;
        return sign && operand.getKind() == ZType.Kind.CHAR && !operand.isArray() ? ZType.INT : operand;
    }

    // (int), (double) and (char) convert between int, double and char; nothing else can be cast.
    private ZType resolveCast(UnaryExpressionContext ctx) {
        ZType operand = resolveUnary(ctx.unaryExpression());
        ZType target = ctx.castType().INT() != null ? ZType.INT : ctx.castType().DOUBLE() != null ? ZType.DOUBLE : ZType.CHAR;
        if (!operand.isUnknown() && !operand.isNumeric()) {
            errors.report("Cannot cast a value of type '" + operand + "' to '" + target + "'", ctx);
        }
        return target;
    }

    private ZType resolvePostfix(PostfixExpressionContext ctx) {
        PrimaryExpressionContext primary = ctx.primaryExpression();
        List<PostfixOpContext> ops = ctx.postfixOp();

        if (primary.SUPER() != null) {
            return chain.resolveSuper(primary, ops, superConstructorCallAllowed);
        }
        boolean bareIdentifierCall = primary.IDENTIFIER() != null && !ops.isEmpty() && ops.get(0).LPAREN() != null;
        if (bareIdentifierCall) {
            // foo(args) with no receiver: calls a method of the class being analyzed itself (supports recursion)
            String methodName = primary.IDENTIFIER().getText();
            List<ZType> argTypes = resolveArguments(ops.get(0).argumentList());
            ZType callType = members.callMethod(classSymbolTable, methodName, argTypes, ops.get(0));
            return chain.continueFrom(callType, ops, 1);
        }

        ZType currentType = resolvePrimary(primary);
        return chain.continueFrom(currentType, ops, 0);
    }

    private ZType resolvePrimary(PrimaryExpressionContext ctx) {
        if (ctx.INT_LITERAL() != null) {
            String text = ctx.INT_LITERAL().getText();
            if (!IntegerLiterals.fits(text)) {
                errors.report(IntegerLiterals.outOfRange(text), ctx);
            }
            return ZType.INT;
        }
        if (ctx.DOUBLE_LITERAL() != null) return ZType.DOUBLE;
        if (ctx.CHAR_LITERAL() != null) return ZType.CHAR;
        if (ctx.STRING_LITERAL() != null) return ZType.STRING;
        if (ctx.TRUE() != null || ctx.FALSE() != null) return ZType.BOOLEAN;
        if (ctx.NULL() != null) return ZType.NULL;
        if (ctx.THIS() != null) return classSymbolTable.getOwnType();
        if (ctx.READLN() != null) return ZType.STRING;
        if (ctx.arrayCreation() != null) return new ArrayCreationResolver(this, classSymbolTable, registry, errors).resolve(ctx.arrayCreation());
        if (ctx.NEW() != null) return resolveObjectCreation(ctx);
        if (ctx.IDENTIFIER() != null) return resolveIdentifier(ctx);
        if (ctx.expression() != null) return resolve(ctx.expression());
        return ZType.UNKNOWN;
    }

    private ZType resolveObjectCreation(PrimaryExpressionContext ctx) {
        if (ctx.IDENTIFIER() == null) {
            return ZType.UNKNOWN;
        }
        return members.construct(ctx.IDENTIFIER().getText(), resolveArguments(ctx.argumentList()), ctx);
    }

    private ZType resolveIdentifier(PrimaryExpressionContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        VariableSymbol local = currentScope != null ? currentScope.resolve(name) : null;
        if (local != null) return local.getType();
        VariableSymbol field = members.bareField(name, ctx);
        if (field != null) return field.getType();
        errors.report("Variable '" + name + "' is not declared", ctx);
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
}
