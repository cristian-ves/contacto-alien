package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import org.antlr.v4.runtime.ParserRuleContext;

// Turns the text of string, character and integer tokens into their values.
public final class LiteralDecoder {

    private LiteralDecoder() {
    }

    // Strips the quotes and resolves the escape sequences of a string or character token.
    public static String unquote(String tokenText) {
        String body = tokenText.substring(1, tokenText.length() - 1);
        StringBuilder out = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                i++;
                out.append(switch (body.charAt(i)) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'r' -> '\r';
                    default -> body.charAt(i);
                });
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static String charCode(String tokenText) {
        return Integer.toString(unquote(tokenText).codePointAt(0));
    }

    public static String integer(String tokenText, ParserRuleContext at) {
        try {
            return Long.toString(Long.parseLong(tokenText));
        } catch (NumberFormatException e) {
            throw new GenerationException("Integer literal out of range: " + tokenText, at);
        }
    }
}
