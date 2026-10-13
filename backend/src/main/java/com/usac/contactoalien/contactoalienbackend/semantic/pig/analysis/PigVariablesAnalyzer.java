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
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;

import java.util.List;

// Analyzes variabilesSection: declares every basic variable, series, structure instance and
// object instance into the single global PigSymbolTable, validating each against the shared
// PigImportResult where relevant. Declares each name as soon as it is processed, so a later
// declaration's initializer may reference an earlier one (documented choice; not exercised by
// the reference file, which only uses literal initializers).
public final class PigVariablesAnalyzer {

    private final PigImportResult importResult;
    private final PigSymbolTable symbolTable;
    private final PigErrorReporter reporter;
    private final PigExpressionTypeResolver expressionResolver;

    public PigVariablesAnalyzer(PigImportResult importResult, PigSymbolTable symbolTable,
                                PigErrorReporter reporter, PigExpressionTypeResolver expressionResolver) {
        this.importResult = importResult;
        this.symbolTable = symbolTable;
        this.reporter = reporter;
        this.expressionResolver = expressionResolver;
    }

    public void analyze(PigParser.VariabilesSectionContext ctx) {
        if (ctx == null) return; // variabilesSection is optional
        for (PigParser.VariableDeclContext declCtx : ctx.variableDecl()) {
            if (declCtx.basicVarDecl() != null) analyzeBasicVarDecl(declCtx.basicVarDecl());
            else if (declCtx.arrayDecl() != null) analyzeArrayDecl(declCtx.arrayDecl());
            else if (declCtx.structInstanceDecl() != null) analyzeStructInstanceDecl(declCtx.structInstanceDecl());
            else if (declCtx.objectInstanceDecl() != null) analyzeObjectInstanceDecl(declCtx.objectInstanceDecl());
        }
    }

    private void analyzeBasicVarDecl(PigParser.BasicVarDeclContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        PigParser.ExpressionContext initExpr = ctx.expression();

        PigType declaredType;
        if (ctx.tipoBasico() != null) {
            declaredType = PigTypeMapper.mapTipoBasico(ctx.tipoBasico());
            if (initExpr != null) {
                PigType initType = expressionResolver.resolve(initExpr);
                if (!PigTypeCompatibility.isAssignable(declaredType, initType)) {
                    reporter.report("cannot initialize variable '" + name + "' of type " + declaredType
                            + " with a value of type " + initType, ctx);
                }
            }
        } else if (initExpr != null) {
            // No explicit tipoBasico: the variable's type is inferred from its initializer.
            // Spec-mandated case: boolean literal -> BOOL. Extended symmetrically to every other
            // initializer (including 'novus X(...)', since basicVarDecl's bare-expression branch
            // matches object instantiation before objectInstanceDecl ever gets a chance to - see
            // the discovery noted in this prompt's response).
            declaredType = expressionResolver.resolve(initExpr);
        } else {
            // Neither a type nor a value: cannot happen once the grammar has parsed
            // successfully; kept only as a defensive fallback.
            declaredType = PigType.UNKNOWN;
        }

        if (!symbolTable.declare(new PigVariableSymbol(name, declaredType))) {
            reporter.report("variable '" + name + "' is already declared", ctx);
        }
    }

    private void analyzeArrayDecl(PigParser.ArrayDeclContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        PigType elementType = PigTypeMapper.mapTipoBasico(ctx.tipoBasico());
        PigType arrayType = PigType.ofArray(elementType);

        for (PigParser.ExpressionContext elementExpr : ctx.expression()) {
            PigType elementValueType = expressionResolver.resolve(elementExpr);
            if (!PigTypeCompatibility.isAssignable(elementType, elementValueType)) {
                reporter.report("cannot initialize an element of series '" + name + "' (type " + elementType
                        + ") with a value of type " + elementValueType, ctx);
            }
        }

        if (!symbolTable.declare(new PigVariableSymbol(name, arrayType))) {
            reporter.report("variable '" + name + "' is already declared", ctx);
        }
    }

    private void analyzeStructInstanceDecl(PigParser.StructInstanceDeclContext ctx) {
        String name = ctx.IDENTIFIER(0).getText();
        String structTypeName = ctx.IDENTIFIER(1).getText();

        YStructSymbol struct = importResult.getYSymbols().resolveStruct(structTypeName);
        if (struct == null) {
            reporter.report("structure '" + structTypeName + "' is not declared", ctx);
        }

        List<PigParser.ExpressionContext> initValues = ctx.expression();
        for (PigParser.ExpressionContext valueExpr : initValues) {
            expressionResolver.resolve(valueExpr); // cascading check regardless of struct resolution
        }
        if (struct != null && !initValues.isEmpty() && initValues.size() != struct.getFieldCount()) {
            reporter.report("structure '" + structTypeName + "' has " + struct.getFieldCount()
                    + " field(s) but " + initValues.size() + " value(s) were given", ctx);
        }
        // Only the positional value COUNT is validated here. Deep per-field type checking
        // (including structures nested inside structures) is not implemented in this prompt;
        // see the final summary for this decision.

        PigType type = struct == null ? PigType.UNKNOWN : PigType.ofStruct(structTypeName);
        if (!symbolTable.declare(new PigVariableSymbol(name, type))) {
            reporter.report("variable '" + name + "' is already declared", ctx);
        }
    }

    private void analyzeObjectInstanceDecl(PigParser.ObjectInstanceDeclContext ctx) {
        // Kept for grammar completeness. In practice, 'esto X : novus Y(...);' is fully matched
        // by basicVarDecl's bare-expression branch first (see the discovery noted in this
        // prompt's response), so this path is likely unreachable with the current grammar.
        String name = ctx.IDENTIFIER(0).getText();
        String className = ctx.IDENTIFIER(1).getText();

        List<PigParser.ExpressionContext> args = ctx.argumentList() == null
                ? List.of() : ctx.argumentList().expression();
        for (PigParser.ExpressionContext arg : args) {
            expressionResolver.resolve(arg);
        }

        PigType type;
        if (!importResult.getClassRegistry().contains(className)) {
            reporter.report("class '" + className + "' is not declared", ctx);
            type = PigType.UNKNOWN;
        } else {
            ClassSymbolTable classTable = importResult.getClassRegistry().resolve(className);
            List<ConstructorSymbol> constructors = classTable.getConstructors();
            boolean matches = constructors.isEmpty() ? args.isEmpty()
                    : constructors.stream().anyMatch(c -> c.getArity() == args.size());
            if (!matches) {
                reporter.report("constructor of class '" + className + "' does not accept " + args.size() + " argument(s)", ctx);
            }
            type = PigType.ofObject(className);
        }

        if (!symbolTable.declare(new PigVariableSymbol(name, type))) {
            reporter.report("variable '" + name + "' is already declared", ctx);
        }
    }
}