package com.usac.contactoalien.contactoalienbackend.api.tokenize;

import com.usac.contactoalien.contactoalienbackend.errors.CollectingErrorListener;
import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigLexer;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YLexer;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TokenizerService {

    public TokenizeResponse tokenize(TokenizeRequest request) {
        Lexer lexer = buildLexer(request.language(), request.code());

        CollectingErrorListener errorListener = new CollectingErrorListener(ErrorPhase.LEXICAL);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        List<TokenDto> tokens = new ArrayList<>();
        Token token = lexer.nextToken();
        while (token.getType() != Token.EOF) {
            tokens.add(toDto(lexer, token));
            token = lexer.nextToken();
        }

        return new TokenizeResponse(tokens, errorListener.getErrors());
    }

    // One lexer instance per supported language; unknown language fails fast rather
    // than silently defaulting to one of the three.
    private Lexer buildLexer(String language, String code) {
        CharStream input = CharStreams.fromString(code);
        return switch (language) {
            case "zetariano" -> new ZetarianoLexer(input);
            case "y" -> new YLexer(input);
            case "pig" -> new PigLexer(input);
            default -> throw new IllegalArgumentException(
                    "Unknown language: " + language + " (expected zetariano, y or pig)");
        };
    }

    private TokenDto toDto(Lexer lexer, Token token) {
        String symbolicName = lexer.getVocabulary().getSymbolicName(token.getType());
        int startColumn = token.getCharPositionInLine();
        int endColumn = startColumn + token.getText().length();
        boolean hidden = token.getChannel() != Token.DEFAULT_CHANNEL;
        return new TokenDto(symbolicName, token.getText(), token.getLine(), startColumn, endColumn, hidden);
    }
}