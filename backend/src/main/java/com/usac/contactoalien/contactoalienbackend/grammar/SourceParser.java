package com.usac.contactoalien.contactoalienbackend.grammar;

import com.usac.contactoalien.contactoalienbackend.errors.CollectingErrorListener;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigLexer;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YLexer;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoLexer;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
Utility parser responsible for building lexers and parsers for Y? and Zetariano sources,
attaching collecting error listeners, and returning structured parse outcomes.
*/
public final class SourceParser {

    private SourceParser() {
        // Utility class
    }

    /**
    Parses a Pig Latin source string into a {@link PigParser.ProgramContext} parse tree.

    @param code The Pig Latin source code.
    @return A {@link ParseOutcome} containing the parse tree and any lexical/syntactic errors.
    */
    public static ParseOutcome<PigParser.ProgramContext> parsePig(String code) {
        CollectingErrorListener lexicalListener = new CollectingErrorListener(ErrorPhase.LEXICAL);
        PigLexer lexer = new PigLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexicalListener);

        CollectingErrorListener syntaxListener = new CollectingErrorListener(ErrorPhase.SYNTACTIC);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PigParser parser = new PigParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxListener);

        PigParser.ProgramContext tree = parser.program();

        List<CompilerError> errors = new ArrayList<>(lexicalListener.getErrors());
        errors.addAll(syntaxListener.getErrors());

        return new ParseOutcome<>(tree, Collections.unmodifiableList(errors));
    }

    /**
    Parses a Y? source string into a {@link YParser.ProgramContext} parse tree.

    @param code The Y? source code.
    @return A {@link ParseOutcome} containing the parse tree and any lexical/syntactic errors.
    */
    public static ParseOutcome<YParser.ProgramContext> parseY(String code) {
        CollectingErrorListener lexicalListener = new CollectingErrorListener(ErrorPhase.LEXICAL);
        YLexer lexer = new YLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexicalListener);

        CollectingErrorListener syntaxListener = new CollectingErrorListener(ErrorPhase.SYNTACTIC);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        YParser parser = new YParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxListener);

        YParser.ProgramContext tree = parser.program();

        List<CompilerError> errors = new ArrayList<>(lexicalListener.getErrors());
        errors.addAll(syntaxListener.getErrors());

        return new ParseOutcome<>(tree, Collections.unmodifiableList(errors));
    }

    /**
    Parses a Zetariano source string into a {@link ZetarianoParser.ClassDeclarationContext} parse tree.

    @param code The Zetariano source code.
    @return A {@link ParseOutcome} containing the parse tree and any lexical/syntactic errors.
    */
    public static ParseOutcome<ZetarianoParser.ClassDeclarationContext> parseZetariano(String code) {
        CollectingErrorListener lexicalListener = new CollectingErrorListener(ErrorPhase.LEXICAL);
        ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexicalListener);

        CollectingErrorListener syntaxListener = new CollectingErrorListener(ErrorPhase.SYNTACTIC);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ZetarianoParser parser = new ZetarianoParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxListener);

        ZetarianoParser.ClassDeclarationContext tree = parser.classDeclaration();

        List<CompilerError> errors = new ArrayList<>(lexicalListener.getErrors());
        errors.addAll(syntaxListener.getErrors());

        return new ParseOutcome<>(tree, Collections.unmodifiableList(errors));
    }
}
