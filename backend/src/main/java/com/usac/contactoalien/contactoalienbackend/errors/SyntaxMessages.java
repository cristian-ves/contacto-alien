package com.usac.contactoalien.contactoalienbackend.errors;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.IntervalSet;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
Turns the raw reports of the ANTLR lexers and parsers into short messages that name what was
found and what was expected, the same way for the three languages. Every user-visible text of
this step lives here.
*/
final class SyntaxMessages {

    // Longer expectation lists say nothing useful, so only "Unexpected X" is shown.
    private static final int MAX_LISTED_EXPECTATIONS = 4;
    private static final String LEXER_REPORT_PREFIX = "token recognition error";

    private static final String UNCLOSED_STRING = "Unclosed string literal: the closing quote is missing before the end of the line";
    private static final String INVALID_CHARACTER = "Invalid character literal: write exactly one character between single quotes";
    private static final String UNCLOSED_COMMENT = "Unclosed comment: the closing mark is missing before the end of the file";
    private static final String UNEXPECTED_CHARACTER = "Unexpected character '%s'";
    private static final String UNEXPECTED_FOUND = "Unexpected %s";
    private static final String UNEXPECTED_END = "Unexpected end of file";
    private static final String EXPECTED_BUT_FOUND = "Expected %s but found %s";
    private static final String END_OF_FILE = "end of file";

    // Readable names for the tokens that have no fixed text, keyed by their name in the grammars.
    private static final Map<String, String> TOKEN_NAMES = Map.ofEntries(
            Map.entry("IDENTIFIER", "an identifier"),
            Map.entry("INT_LITERAL", "an integer"),
            Map.entry("FLOAT_LITERAL", "a decimal number"),
            Map.entry("DOUBLE_LITERAL", "a decimal number"),
            Map.entry("STRING_LITERAL", "a string"),
            Map.entry("CHAR_LITERAL", "a character"),
            Map.entry("NEWLINE", "end of line"),
            Map.entry("INDENT", "an indented block"),
            Map.entry("DEDENT", "the end of a block"),
            Map.entry("VARIABILES_SECTION", "'VARIABILES>'"),
            Map.entry("MAIOR_SECTION", "'MAIOR>'"),
            Map.entry("PROGRAM_END", "'FINIS'"));

    // Accepted only to report them as unsupported later, so they are never suggested as the way to continue.
    private static final Set<String> UNSUGGESTED = Set.of("FUNCTIONS_SECTION");

    // Tokens that stand for layout rather than text are named instead of quoted when they are the culprit.
    private static final Map<String, String> FOUND_NAMES = Map.of(
            "NEWLINE", "end of line",
            "INDENT", "extra indentation",
            "DEDENT", "end of block");

    private SyntaxMessages() {
    }

    static String message(Object recognizer, Object offendingSymbol, String raw, RecognitionException cause) {
        if (recognizer instanceof Lexer lexer) {
            return lexical(lexer, raw);
        }
        if (recognizer instanceof Parser parser && offendingSymbol instanceof Token found) {
            return syntactic(parser, found, cause);
        }
        return raw;
    }

    private static String lexical(Lexer lexer, String raw) {
        if (!raw.startsWith(LEXER_REPORT_PREFIX)) {
            return raw;
        }
        int start = lexer._tokenStartCharIndex;
        int stop = Math.min(lexer.getInputStream().index(), lexer.getInputStream().size() - 1);
        String text = lexer.getInputStream().getText(Interval.of(start, Math.max(start, stop)));
        if (text.startsWith("\"")) {
            return UNCLOSED_STRING;
        }
        if (text.startsWith("'")) {
            return INVALID_CHARACTER;
        }
        if (text.startsWith("/*") || text.startsWith("##")) {
            return UNCLOSED_COMMENT;
        }
        return UNEXPECTED_CHARACTER.formatted(text.isEmpty() ? "" : text.substring(0, 1));
    }

    private static String syntactic(Parser parser, Token found, RecognitionException cause) {
        List<String> expected = expectedNames(parser, cause);
        boolean listable = !expected.isEmpty() && expected.size() <= MAX_LISTED_EXPECTATIONS;
        if (found.getType() == Token.EOF) {
            return listable ? UNEXPECTED_END + ", expected " + join(expected) : UNEXPECTED_END;
        }
        String foundName = describe(parser.getVocabulary(), found);
        return listable ? EXPECTED_BUT_FOUND.formatted(join(expected), foundName) : UNEXPECTED_FOUND.formatted(foundName);
    }

    private static List<String> expectedNames(Parser parser, RecognitionException cause) {
        IntervalSet set = cause != null && cause.getExpectedTokens() != null
                ? cause.getExpectedTokens() : parser.getExpectedTokens();
        Set<String> names = new LinkedHashSet<>();
        for (int type : set.toList()) {
            if (UNSUGGESTED.contains(parser.getVocabulary().getSymbolicName(type))) {
                continue;
            }
            names.add(typeName(parser.getVocabulary(), type));
        }
        return new ArrayList<>(names);
    }

    private static String describe(Vocabulary vocabulary, Token token) {
        String symbolic = vocabulary.getSymbolicName(token.getType());
        if (symbolic != null && FOUND_NAMES.containsKey(symbolic)) {
            return FOUND_NAMES.get(symbolic);
        }
        return "'" + token.getText() + "'";
    }

    private static String typeName(Vocabulary vocabulary, int type) {
        if (type == Token.EOF) {
            return END_OF_FILE;
        }
        String symbolic = vocabulary.getSymbolicName(type);
        if (symbolic != null && TOKEN_NAMES.containsKey(symbolic)) {
            return TOKEN_NAMES.get(symbolic);
        }
        String literal = vocabulary.getLiteralName(type);
        return literal != null ? literal : String.valueOf(symbolic);
    }

    private static String join(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " or " + names.get(names.size() - 1);
    }
}
