package com.usac.contactoalien.contactoalienbackend.semantic.pig.imports;

import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.grammar.ParseOutcome;
import com.usac.contactoalien.contactoalienbackend.grammar.SourceParser;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParserBaseVisitor;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.semantic.analysis.SemanticAnalyzer;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.SemanticError;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigSemanticError;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.analysis.YSemanticAnalyzer;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YSemanticError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Resolves and loads every import of one Pig Latin file against a virtual file map: .z imports
// are lexed, parsed and registered into one shared CompilationUnitRegistry (so imported classes
// can reference each other, same as within a single Zetariano compilation batch); .y imports are
// lexed, parsed, fully analyzed with YSemanticAnalyzer, and their symbols merged into one shared
// ImportedYSymbols. All .z symbol collection happens before any .z body analysis, so imported
// classes can reference each other regardless of import order.
public final class PigImportLoader {

    public PigImportResult load(List<PigParser.ImportDeclContext> imports, Map<String, String> virtualFiles) {
        List<PigSemanticError> errors = new ArrayList<>();
        CompilationUnitRegistry classRegistry = new CompilationUnitRegistry();
        ImportedYSymbols ySymbols = new ImportedYSymbols();
        List<PendingZetariano> pendingClasses = new ArrayList<>();

        for (PigParser.ImportDeclContext importCtx : imports) {
            ImportPathResolver.ResolvedImport resolved = ImportPathResolver.resolve(importCtx);
            String content = virtualFiles.get(resolved.getVirtualPath());
            if (content == null) {
                errors.add(new PigSemanticError("imported file not found: " + resolved.getVirtualPath(), 1, 0));
                continue;
            }
            if (resolved.getLanguage() == ImportPathResolver.Language.Z) {
                loadZetarianoImport(resolved, content, classRegistry, pendingClasses, errors);
            } else {
                loadYImport(resolved, content, ySymbols, errors);
            }
        }

        // Every .z import is registered by now; analyze bodies with the full registry available.
        for (PendingZetariano pending : pendingClasses) {
            pending.analyzer.analyzeBodies(pending.classTree, pending.classTable, classRegistry);
            for (SemanticError e : pending.analyzer.getErrors().getErrors()) {
                errors.add(new PigSemanticError("[" + pending.fileLabel + "] " + e.getMessage(), e.getLine(), e.getColumn()));
            }
        }

        return new PigImportResult(classRegistry, ySymbols, Collections.unmodifiableList(errors));
    }

    private void loadZetarianoImport(ImportPathResolver.ResolvedImport resolved, String content,
                                     CompilationUnitRegistry classRegistry,
                                     List<PendingZetariano> pendingClasses, List<PigSemanticError> errors) {
        ParseOutcome<ZetarianoParser.ClassDeclarationContext> outcome = SourceParser.parseZetariano(content);
        if (outcome.hasErrors()) {
            for (CompilerError e : outcome.errors()) {
                errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] " + e.message(), e.line(), e.column()));
            }
            return;
        }

        ZetarianoParser.ClassDeclarationContext classTree = outcome.tree();
        if (classTree == null || classTree.IDENTIFIER() == null) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] could not parse class declaration", 1, 0));
            return;
        }

        String actualClassName = classTree.IDENTIFIER().getText();
        if (!actualClassName.equals(resolved.getExpectedModuleName())) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] class name '" + actualClassName
                    + "' does not match expected module name '" + resolved.getExpectedModuleName() + "'", 1, 0));
        }

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        ClassSymbolTable classTable = analyzer.collectSymbols(classTree, classRegistry);
        classRegistry.register(classTable);

        pendingClasses.add(new PendingZetariano(resolved.getVirtualPath(), classTree, classTable, analyzer));
    }

    private void loadYImport(ImportPathResolver.ResolvedImport resolved, String content,
                             ImportedYSymbols ySymbols, List<PigSemanticError> errors) {
        ParseOutcome<YParser.ProgramContext> outcome = SourceParser.parseY(content);
        if (outcome.hasErrors()) {
            for (CompilerError e : outcome.errors()) {
                errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] " + e.message(), e.line(), e.column()));
            }
            return;
        }

        YParser.ProgramContext programTree = outcome.tree();
        if (programTree == null) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] could not parse Y program", 1, 0));
            return;
        }

        YSemanticAnalyzer analyzer = new YSemanticAnalyzer();
        analyzer.analyze(programTree);

        List<String> structNames = collectAllStructNames(programTree);
        List<String> functionNames = new ArrayList<>();
        if (programTree.functionsSection() != null && programTree.functionsSection().functionDeclaration() != null) {
            for (YParser.FunctionDeclarationContext fn : programTree.functionsSection().functionDeclaration()) {
                functionNames.add(fn.IDENTIFIER().getText());
            }
        }

        List<String> collisions = ySymbols.addFrom(analyzer.getStructRegistry(), structNames,
                analyzer.getFunctionRegistry(), functionNames);
        for (String collision : collisions) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] " + collision, 1, 0));
        }
        for (YSemanticError e : analyzer.getErrors().getErrors()) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] " + e.getMessage(), e.getLine(), e.getColumn()));
        }
    }

    // Mirrors YStructCollector's own traversal (top-level, nested-in-struct, and local-in-function
    // structure declarations) so every name that ended up in the file's YStructRegistry is found.
    private static List<String> collectAllStructNames(YParser.ProgramContext programTree) {
        List<String> names = new ArrayList<>();
        programTree.accept(new YParserBaseVisitor<Void>() {
            @Override
            public Void visitStructDeclaration(YParser.StructDeclarationContext ctx) {
                names.add(ctx.IDENTIFIER().getText());
                return visitChildren(ctx);
            }
        });
        return names;
    }

    private static final class PendingZetariano {
        final String fileLabel;
        final ZetarianoParser.ClassDeclarationContext classTree;
        final ClassSymbolTable classTable;
        final SemanticAnalyzer analyzer;

        PendingZetariano(String fileLabel, ZetarianoParser.ClassDeclarationContext classTree,
                         ClassSymbolTable classTable, SemanticAnalyzer analyzer) {
            this.fileLabel = fileLabel;
            this.classTree = classTree;
            this.classTable = classTable;
            this.analyzer = analyzer;
        }
    }
}