package com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis;

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

    public List<String> analyze(PigParser.ProgramContext program, PigImportResult importResult) {
        PigErrorReporter reporter = new PigErrorReporter();
        PigSymbolTable symbolTable = new PigSymbolTable();
        PigExpressionTypeResolver expressionResolver = new PigExpressionTypeResolver(importResult, symbolTable, reporter);

        new PigVariablesAnalyzer(importResult, symbolTable, reporter, expressionResolver).analyze(program.variabilesSection());
        new PigStatementAnalyzer(importResult, symbolTable, reporter, expressionResolver).analyze(program.maiorSection());

        List<String> combined = new ArrayList<>(importResult.getErrors());
        for (PigSemanticError error : reporter.getErrors()) {
            combined.add(error.toString());
        }
        return combined;
    }
}