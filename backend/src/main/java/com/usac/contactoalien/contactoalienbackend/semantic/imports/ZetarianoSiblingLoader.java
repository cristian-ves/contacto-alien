package com.usac.contactoalien.contactoalienbackend.semantic.imports;

import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.grammar.ParseOutcome;
import com.usac.contactoalien.contactoalienbackend.grammar.SourceParser;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.semantic.analysis.SemanticAnalyzer;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
Loads sibling Zetariano classes from the virtual imports map into a CompilationUnitRegistry
so cross-class types resolve properly during semantic analysis. Parses siblings, extracts
class names to pre-register empty symbol tables, and collects full member signatures in a
second pass. Bodies of sibling classes are never analyzed here.
*/
public final class ZetarianoSiblingLoader {

    public record LoadResult(CompilationUnitRegistry registry, List<CompilerError> errors) {
        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
    }

    private record ParsedSibling(String filePath, ZetarianoParser.ClassDeclarationContext classTree, String className) {}

    public LoadResult load(Map<String, String> imports, String currentClassName) {
        return load(imports, currentClassName, null);
    }

    // currentPath is the location of the file under analysis, or null when it sits with its siblings.
    public LoadResult load(Map<String, String> imports, String currentClassName, String currentPath) {
        if (imports == null || imports.isEmpty()) {
            return new LoadResult(new CompilationUnitRegistry(), Collections.emptyList());
        }

        List<CompilerError> syntaxErrors = new ArrayList<>();
        List<ParsedSibling> parsedSiblings = new ArrayList<>();

        for (Map.Entry<String, String> entry : imports.entrySet()) {
            String filePath = entry.getKey();
            String source = entry.getValue();

            ParseOutcome<ZetarianoParser.ClassDeclarationContext> outcome = SourceParser.parseZetariano(source);
            if (outcome.hasErrors()) {
                for (CompilerError e : outcome.errors()) {
                    syntaxErrors.add(new CompilerError(
                            e.line(),
                            e.column(),
                            "[" + filePath + "] " + e.message(),
                            e.phase()
                    ));
                }
                continue;
            }

            ZetarianoParser.ClassDeclarationContext classTree = outcome.tree();
            if (classTree == null || classTree.IDENTIFIER(0) == null) {
                continue;
            }

            String siblingClassName = classTree.IDENTIFIER(0).getText();
            if (siblingClassName.equals(currentClassName)) {
                continue;
            }

            parsedSiblings.add(new ParsedSibling(filePath, classTree, siblingClassName));
        }

        if (!syntaxErrors.isEmpty()) {
            return new LoadResult(null, Collections.unmodifiableList(syntaxErrors));
        }

        CompilationUnitRegistry registry = new CompilationUnitRegistry();

        // Registering empty tables for every class first makes mutual references (e.g. Nodo and
        // ListaDoble) resolvable, since TypeResolver only checks registry.contains during symbol collection.
        for (ParsedSibling sibling : parsedSiblings) {
            registry.register(new ClassSymbolTable(sibling.className()));
        }
        if (currentClassName != null && !currentClassName.isEmpty()) {
            ClassSymbolTable placeholder = new ClassSymbolTable(currentClassName);
            placeholder.setPackageName(currentPath != null ? PackagePaths.of(currentPath) : PackagePaths.ofSiblings(imports));
            registry.register(placeholder);
        }

        // The second pass replaces each empty table with the full one containing fields and method signatures.
        for (ParsedSibling sibling : parsedSiblings) {
            SemanticAnalyzer throwawayAnalyzer = new SemanticAnalyzer();
            ClassSymbolTable fullTable = throwawayAnalyzer.collectSymbols(sibling.classTree(), registry,
                    PackagePaths.of(sibling.filePath()));
            registry.register(fullTable);
        }

        return new LoadResult(registry, Collections.emptyList());
    }
}
