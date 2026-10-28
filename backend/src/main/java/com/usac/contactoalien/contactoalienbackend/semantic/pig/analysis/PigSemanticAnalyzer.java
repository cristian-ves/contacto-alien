package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigSemanticError;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.symbols.PigSymbolTable;

import java.util.ArrayList;
import java.util.List;

// Entry point for analyzing the body (VARIABILES>/MAIOR>) of one Pig Latin file, against an
// already-loaded PigImportResult. Returns a single combined error list: the import errors as-is
// (already file-prefixed), followed by the body's own errors, each with its own line/column.
public final class PigSemanticAnalyzer {

    private static final String FUNCTIONS_NOT_IN_PIG =
            "Pig Latin does not define functions; declare them in an imported .y file";

    public List<PigSemanticError> analyze(PigParser.ProgramContext program, PigImportResult importResult) {
        // An import that does not parse leaves its classes and functions unknown; checking the body against
        // them would only add one "not declared" error per use.
        boolean importsBroken = importResult.getErrors().stream()
                .anyMatch(e -> e.getPhase() != ErrorPhase.SEMANTIC);
        if (importsBroken) {
            return new ArrayList<>(importResult.getErrors());
        }
        PigErrorReporter reporter = new PigErrorReporter();
        if (program.functionsSection() != null) {
            // The skipped function bodies would turn every call into a "not declared" error; one error is enough.
            reporter.report(FUNCTIONS_NOT_IN_PIG, program.functionsSection());
            List<PigSemanticError> only = new ArrayList<>(importResult.getErrors());
            only.addAll(reporter.getErrors());
            return only;
        }
        PigSymbolTable symbolTable = new PigSymbolTable();
        PigExpressionTypeResolver expressionResolver = new PigExpressionTypeResolver(importResult, symbolTable, reporter);

        new PigVariablesAnalyzer(importResult, symbolTable, reporter, expressionResolver).analyze(program.variabilesSection());
        new PigStatementAnalyzer(importResult, symbolTable, reporter, expressionResolver).analyze(program.maiorSection());

        List<PigSemanticError> combined = new ArrayList<>(importResult.getErrors());
        combined.addAll(reporter.getErrors());
        return combined;
    }
}