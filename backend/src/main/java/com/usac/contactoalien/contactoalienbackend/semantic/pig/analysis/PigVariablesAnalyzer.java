package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigType;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.types.PigTypeCompatibility;
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
    private final PigCollectionRules collections;

    public PigVariablesAnalyzer(PigImportResult importResult, PigSymbolTable symbolTable,
                                PigErrorReporter reporter, PigExpressionTypeResolver expressionResolver) {
        this.importResult = importResult;
        this.symbolTable = symbolTable;
        this.reporter = reporter;
        this.expressionResolver = expressionResolver;
        this.collections = new PigCollectionRules(reporter);
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
                if (!PigTypeCompatibility.isAssignable(declaredType, initType, importResult.getClassRegistry())) {
                    reporter.report("cannot initialize variable '" + name + "' of type " + declaredType
                            + " with a value of type " + initType, ctx);
                }
            }
        } else if (initExpr != null) {
            // No explicit tipoBasico: the variable's type is inferred from its initializer.
            // Spec-mandated case: boolean literal -> BOOL. Extended symmetrically to every other
            // initializer (including 'novus X(...)', since basicVarDecl's bare-expression branch
            // matches object instantiation before objectInstanceDecl ever gets a chance to).
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
        String name = ctx.IDENTIFIER(0).getText();
        PigType elementType = arrayElementType(ctx);
        PigType arrayType = PigType.ofArray(elementType);
        collections.checkSeriesLength(ctx);

        for (PigParser.ExpressionContext elementExpr : ctx.expression()) {
            PigType elementValueType = expressionResolver.resolve(elementExpr);
            if (!PigTypeCompatibility.isAssignable(elementType, elementValueType, importResult.getClassRegistry())) {
                reporter.report("cannot initialize an element of series '" + name + "' (type " + elementType
                        + ") with a value of type " + elementValueType, ctx);
            }
        }

        if (!symbolTable.declare(new PigVariableSymbol(name, arrayType, IntegerLiterals.length(ctx.INT_LITERAL().getText())))) {
            reporter.report("variable '" + name + "' is already declared", ctx);
        }
    }

    // A series holds basic values, structures of an imported .y or objects of an imported .z class.
    private PigType arrayElementType(PigParser.ArrayDeclContext ctx) {
        if (ctx.tipoBasico() != null) {
            return PigTypeMapper.mapTipoBasico(ctx.tipoBasico());
        }
        String typeName = ctx.IDENTIFIER(1).getText();
        if (importResult.getYSymbols().resolveStruct(typeName) != null) {
            return PigType.ofStruct(typeName);
        }
        if (importResult.getClassRegistry().contains(typeName)) {
            return PigType.ofObject(typeName);
        }
        reporter.report("type '" + typeName + "' is not declared", ctx);
        return PigType.UNKNOWN;
    }

    private void analyzeStructInstanceDecl(PigParser.StructInstanceDeclContext ctx) {
        String name = ctx.IDENTIFIER(0).getText();
        String structTypeName = ctx.IDENTIFIER(1).getText();

        YStructSymbol struct = importResult.getYSymbols().resolveStruct(structTypeName);
        if (struct == null) {
            reporter.report("structure '" + structTypeName + "' is not declared", ctx);
        }

        List<PigParser.ExpressionContext> initValues = ctx.expression();
        List<PigType> valueTypes = new java.util.ArrayList<>();
        for (PigParser.ExpressionContext valueExpr : initValues) {
            valueTypes.add(expressionResolver.resolve(valueExpr)); // cascading check regardless of struct resolution
        }
        if (struct != null) {
            collections.checkStructValues(struct, valueTypes, ctx);
        }
        if (struct != null && !initValues.isEmpty() && initValues.size() != struct.getFieldCount()) {
            reporter.report("structure '" + structTypeName + "' has " + struct.getFieldCount()
                    + " field(s) but " + initValues.size() + " value(s) were given", ctx);
        }
        // The count and the type of each positional value are validated; nested structures are not.

        PigType type = struct == null ? PigType.UNKNOWN : PigType.ofStruct(structTypeName);
        if (!symbolTable.declare(new PigVariableSymbol(name, type))) {
            reporter.report("variable '" + name + "' is already declared", ctx);
        }
    }

    // `esto o : novus Sub(args) : Base;`, the same without `: Base`, and `esto o : null : Base;`.
    // The variable has the declared class; the created object must be of that class or a subclass.
    private void analyzeObjectInstanceDecl(PigParser.ObjectInstanceDeclContext ctx) {
        String name = ctx.IDENTIFIER(0).getText();
        PigType created = null;
        if (ctx.NOVUS() != null) {
            List<PigParser.ExpressionContext> args = ctx.argumentList() == null
                    ? List.of() : ctx.argumentList().expression();
            created = expressionResolver.resolveNew(ctx.IDENTIFIER(1).getText(), args, ctx);
        }
        boolean hasDeclaredType = ctx.NULL() != null || ctx.IDENTIFIER().size() > 2;
        PigType type = created;
        if (hasDeclaredType) {
            String declaredName = ctx.IDENTIFIER(ctx.IDENTIFIER().size() - 1).getText();
            type = declaredClass(declaredName, ctx);
            if (created != null && !created.isUnknown() && !PigTypeCompatibility.isAssignable(type, created, importResult.getClassRegistry())) {
                reporter.report("class '" + created.getClassName() + "' is not a subclass of '" + declaredName + "'", ctx);
            }
        }
        if (!symbolTable.declare(new PigVariableSymbol(name, type))) {
            reporter.report("variable '" + name + "' is already declared", ctx);
        }
    }

    private PigType declaredClass(String className, PigParser.ObjectInstanceDeclContext ctx) {
        if (!importResult.getClassRegistry().contains(className)) {
            reporter.report("class '" + className + "' is not declared", ctx);
            return PigType.UNKNOWN;
        }
        return PigType.ofObject(className);
    }
}
