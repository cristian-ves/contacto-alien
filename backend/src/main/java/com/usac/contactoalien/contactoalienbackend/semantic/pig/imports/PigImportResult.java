package com.usac.contactoalien.contactoalienbackend.semantic.pig.imports;

import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigSemanticError;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;

import java.util.List;

// Outcome of loading every import of one Pig Latin file: the shared registry of every imported
// Zetariano class, the accumulated symbols from every imported Y? file, and every error found
// across all imports, each already prefixed with the file it came from.
public final class PigImportResult {

    private final CompilationUnitRegistry classRegistry;
    private final ImportedYSymbols ySymbols;
    private final List<PigSemanticError> errors;

    public PigImportResult(CompilationUnitRegistry classRegistry, ImportedYSymbols ySymbols, List<PigSemanticError> errors) {
        this.classRegistry = classRegistry;
        this.ySymbols = ySymbols;
        this.errors = errors;
    }

    public CompilationUnitRegistry getClassRegistry() { return classRegistry; }
    public ImportedYSymbols getYSymbols() { return ySymbols; }
    public List<PigSemanticError> getErrors() { return errors; }
}