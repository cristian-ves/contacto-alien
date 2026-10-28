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
import com.usac.contactoalien.contactoalienbackend.semantic.imports.PackagePaths;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigSemanticError;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.analysis.YSemanticAnalyzer;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YSemanticError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Resolves and loads every import of one Pig Latin file against a virtual file map: .z imports
// are lexed, parsed and registered into one shared CompilationUnitRegistry (so imported classes
// can reference each other, same as within a single Zetariano compilation batch); .y imports are
// lexed, parsed, fully analyzed with YSemanticAnalyzer, and their symbols merged into one shared
// ImportedYSymbols. All .z symbol collection happens before any .z body analysis, so imported
// classes can reference each other regardless of import order.
public final class PigImportLoader {

    private static final String NAME_MISMATCH = "does not match expected module name";

    public PigImportResult load(List<PigParser.ImportDeclContext> imports, Map<String, String> virtualFiles) {
        List<PigSemanticError> errors = new ArrayList<>();
        CompilationUnitRegistry classRegistry = new CompilationUnitRegistry();
        ImportedYSymbols ySymbols = new ImportedYSymbols();
        List<PendingZetariano> pendingClasses = new ArrayList<>();

        for (PigParser.ImportDeclContext importCtx : imports) {
            ImportPathResolver.ResolvedImport resolved = ImportPathResolver.resolve(importCtx);
            String content = virtualFiles.get(resolved.getVirtualPath());
            if (content == null) {
                errors.add(new PigSemanticError("imported file not found: " + resolved.getVirtualPath(),
                        importCtx.getStart().getLine(), importCtx.getStart().getCharPositionInLine() + 1));
                continue;
            }
            if (resolved.getLanguage() == ImportPathResolver.Language.Z) {
                loadZetarianoImport(resolved, content, classRegistry, pendingClasses, errors);
            } else {
                loadYImport(resolved, content, ySymbols, errors);
            }
        }

        // Every .z import is registered by now; analyze bodies with the full registry available.
        // A class whose name differs from its file makes every reference to it unknown; that one error says it all.
        boolean nameMismatch = errors.stream().anyMatch(e -> e.getMessage().contains(NAME_MISMATCH));
        Set<String> reported = new HashSet<>();
        for (PendingZetariano pending : pendingClasses) {
            if (nameMismatch) {
                break;
            }
            pending.analyzer.analyzeBodies(pending.classTree, pending.classTable, classRegistry);
            for (SemanticError e : pending.analyzer.getErrors().getErrors()) {
                String label = "[" + pending.fileLabel + "] " + e.getMessage();
                if (reported.add(label + e.getLine() + ":" + e.getColumn())) {
                    errors.add(new PigSemanticError(label, e.getLine(), e.getColumn()));
                }
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
                errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] " + e.message(), e.line(), e.column(), e.phase()));
            }
            return;
        }

        ZetarianoParser.ClassDeclarationContext classTree = outcome.tree();
        if (classTree == null || classTree.IDENTIFIER(0) == null) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] could not parse class declaration", 1, 1));
            return;
        }

        String actualClassName = classTree.IDENTIFIER(0).getText();
        if (!actualClassName.equals(resolved.getExpectedModuleName())) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] class name '" + actualClassName
                    + "' " + NAME_MISMATCH + " '" + resolved.getExpectedModuleName() + "'",
                    classTree.IDENTIFIER(0).getSymbol().getLine(), classTree.IDENTIFIER(0).getSymbol().getCharPositionInLine() + 1));
        }

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        ClassSymbolTable classTable = analyzer.collectSymbols(classTree, classRegistry,
                PackagePaths.of(resolved.getVirtualPath()));
        classRegistry.register(classTable);

        pendingClasses.add(new PendingZetariano(resolved.getVirtualPath(), classTree, classTable, analyzer));
    }

    private void loadYImport(ImportPathResolver.ResolvedImport resolved, String content,
                             ImportedYSymbols ySymbols, List<PigSemanticError> errors) {
        ParseOutcome<YParser.ProgramContext> outcome = SourceParser.parseY(content);
        if (outcome.hasErrors()) {
            for (CompilerError e : outcome.errors()) {
                errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] " + e.message(), e.line(), e.column(), e.phase()));
            }
            return;
        }

        YParser.ProgramContext programTree = outcome.tree();
        if (programTree == null) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] could not parse Y program", 1, 1));
            return;
        }

        YSemanticAnalyzer analyzer = new YSemanticAnalyzer();
        analyzer.analyze(programTree);

        List<String> structNames = new ArrayList<>(new LinkedHashSet<>(collectAllStructNames(programTree)));
        List<String> functionNames = new ArrayList<>();
        if (programTree.functionsSection() != null && programTree.functionsSection().functionDeclaration() != null) {
            for (YParser.FunctionDeclarationContext fn : programTree.functionsSection().functionDeclaration()) {
                String functionName = fn.IDENTIFIER().getText();
                if (!functionNames.contains(functionName)) {
                    functionNames.add(functionName);
                }
            }
        }

        List<String> collisions = ySymbols.addFrom(analyzer.getStructRegistry(), structNames,
                analyzer.getFunctionRegistry(), functionNames);
        for (String collision : collisions) {
            errors.add(new PigSemanticError("[" + resolved.getVirtualPath() + "] " + collision, 1, 1));
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