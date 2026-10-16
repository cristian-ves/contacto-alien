package com.usac.contactoalien.contactoalienbackend.api.validate;

import com.usac.contactoalien.contactoalienbackend.errors.CollectingErrorListener;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;
import com.usac.contactoalien.contactoalienbackend.grammar.ParseOutcome;
import com.usac.contactoalien.contactoalienbackend.grammar.SourceParser;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigLexer;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import com.usac.contactoalien.contactoalienbackend.semantic.analysis.SemanticAnalyzer;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.SemanticError;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.ClassSymbolTable;
import com.usac.contactoalien.contactoalienbackend.semantic.symbols.CompilationUnitRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.analysis.PigSemanticAnalyzer;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportLoader;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.imports.PigImportResult;
import com.usac.contactoalien.contactoalienbackend.semantic.pig.errors.PigSemanticError;
import com.usac.contactoalien.contactoalienbackend.semantic.y.analysis.YSemanticAnalyzer;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YSemanticError;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Runs the full lexical + syntactic + semantic pipeline for one file in one of the three
// languages. Lexical/syntactic errors are collected via CollectingErrorListener attached to
// both the lexer and the parser; semantic errors come from each language's own analyzer.
@Service
public class ValidatorService {

    public ValidateResponse validate(ValidateRequest request) {
        List<CompilerError> errors = switch (request.language()) {
            case "zetariano" -> validateZetariano(request.code());
            case "y" -> validateY(request.code());
            case "pig" -> validatePig(request.code(), request.imports());
            default -> throw new IllegalArgumentException(
                    "Unknown language: " + request.language() + " (expected zetariano, y or pig)");
        };
        return new ValidateResponse(errors.isEmpty(), errors);
    }

    private List<CompilerError> validateZetariano(String code) {
        ParseOutcome<ZetarianoParser.ClassDeclarationContext> outcome = SourceParser.parseZetariano(code);
        if (outcome.hasErrors()) {
            return outcome.errors();
        }

        ZetarianoParser.ClassDeclarationContext classTree = outcome.tree();
        List<CompilerError> errors = new ArrayList<>(outcome.errors());

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        CompilationUnitRegistry registry = new CompilationUnitRegistry();
        ClassSymbolTable classTable = analyzer.collectSymbols(classTree, registry);
        registry.register(classTable);
        analyzer.analyzeBodies(classTree, classTable, registry);

        for (SemanticError e : analyzer.getErrors().getErrors()) {
            errors.add(new CompilerError(e.getLine(), e.getColumn(), e.getMessage(), ErrorPhase.SEMANTIC));
        }
        return errors;
    }

    private List<CompilerError> validateY(String code) {
        ParseOutcome<YParser.ProgramContext> outcome = SourceParser.parseY(code);
        if (outcome.hasErrors()) {
            return outcome.errors();
        }

        YParser.ProgramContext programTree = outcome.tree();
        List<CompilerError> errors = new ArrayList<>(outcome.errors());

        YSemanticAnalyzer analyzer = new YSemanticAnalyzer();
        analyzer.analyze(programTree);

        for (YSemanticError e : analyzer.getErrors().getErrors()) {
            errors.add(new CompilerError(e.getLine(), e.getColumn(), e.getMessage(), ErrorPhase.SEMANTIC));
        }
        return errors;
    }

    private List<CompilerError> validatePig(String code, Map<String, String> imports) {
        Map<String, String> virtualFiles = imports != null ? imports : Collections.emptyMap();

        CollectingErrorListener lexicalListener = new CollectingErrorListener(ErrorPhase.LEXICAL);
        PigLexer lexer = new PigLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexicalListener);

        CollectingErrorListener syntaxListener = new CollectingErrorListener(ErrorPhase.SYNTACTIC);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PigParser parser = new PigParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxListener);

        PigParser.ProgramContext programTree = parser.program();

        List<CompilerError> errors = new ArrayList<>(lexicalListener.getErrors());
        errors.addAll(syntaxListener.getErrors());
        if (!errors.isEmpty()) {
            return errors;
        }

        PigImportResult importResult = new PigImportLoader().load(programTree.importDecl(), virtualFiles);
        List<PigSemanticError> semanticErrors = new PigSemanticAnalyzer().analyze(programTree, importResult);

        for (PigSemanticError e : semanticErrors) {
            errors.add(new CompilerError(e.getLine(), e.getColumn(), e.getMessage(), ErrorPhase.SEMANTIC));
        }
        return errors;
    }
}