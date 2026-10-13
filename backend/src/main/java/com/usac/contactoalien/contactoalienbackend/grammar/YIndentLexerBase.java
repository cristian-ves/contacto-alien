package com.usac.contactoalien.contactoalienbackend.grammar.y;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Base class for the ANTLR-generated YLexer (see "superClass" in YLexer.g4).
 * Owns all indentation-sensitive logic: synthesizing INDENT/DEDENT from
 * NEWLINE tokens, and suppressing that tracking entirely while inside an
 * explicit '{' '}' block (only "elegir" uses braces in Y?), the same way
 * Python suppresses indentation inside (), [], {}.
 *
 * ALGORITHM (indentation)
 * NEWLINE matches the line break plus the spaces opening the next line, so
 * its width is known immediately. A stack of open indentation widths
 * (starting at 0) is compared against that width: wider pushes and emits
 * INDENT, narrower pops and emits one DEDENT per level until an exact match
 * is found (or an indentation error is raised), equal emits nothing. EOF
 * flushes any remaining open levels as DEDENTs. Blank and comment-only
 * lines never affect the stack.
 *
 * BRACE SUPPRESSION
 * LBRACE/RBRACE are ordinary tokens already recognized by the grammar; this
 * class just counts them. While braceDepth > 0, NEWLINE is still emitted
 * (YParser.g4 needs it as a statement separator inside "elegir"), but the
 * indentation stack is never touched and no INDENT/DEDENT is produced,
 * regardless of how the code inside the braces is visually indented.
 */
public abstract class YIndentLexerBase extends Lexer {

    private final Deque<Integer> indentStack = new ArrayDeque<>();
    private final Queue<Token> pendingTokens = new LinkedList<>();
    private boolean hasSeenFirstStatement = false;
    private boolean eofHandled = false;

    /** Depth of currently open explicit '{' '}' blocks. */
    private int braceDepth = 0;

    protected YIndentLexerBase(CharStream input) {
        super(input);
        indentStack.push(0);
    }

    @Override
    public Token nextToken() {
        if (!pendingTokens.isEmpty()) {
            return pendingTokens.poll();
        }

        Token next = super.nextToken();

        if (next.getType() == YLexer.LBRACE) {
            braceDepth++;
            hasSeenFirstStatement = true;
            return next;
        }

        if (next.getType() == YLexer.RBRACE) {
            if (braceDepth > 0) {
                braceDepth--;
            }
            hasSeenFirstStatement = true;
            return next;
        }

        if (next.getType() == Token.EOF) {
            handleEndOfFile(next);
            return pendingTokens.poll();
        }

        if (next.getType() == YLexer.NEWLINE) {
            handleNewline(next);
            return pendingTokens.isEmpty() ? nextToken() : pendingTokens.poll();
        }

        hasSeenFirstStatement = true;
        return next;
    }

    private void handleNewline(Token newlineToken) {
        if (isBlankOrCommentOnlyAhead()) {
            return;
        }

        if (braceDepth > 0) {
            // Inside explicit braces: pass NEWLINE through, but never touch
            // the indentation stack.
            pendingTokens.add(newlineToken);
            return;
        }

        if (!hasSeenFirstStatement) {
            return;
        }

        int width = widthOf(newlineToken);
        pendingTokens.add(newlineToken);

        int currentLevel = indentStack.peek();
        if (width > currentLevel) {
            indentStack.push(width);
            pendingTokens.add(syntheticToken(YLexer.INDENT, "<INDENT>", newlineToken));
        } else if (width < currentLevel) {
            while (indentStack.peek() > width) {
                indentStack.pop();
                pendingTokens.add(syntheticToken(YLexer.DEDENT, "<DEDENT>", newlineToken));
            }
            if (indentStack.peek() != width) {
                throw new IllegalStateException(
                        "Y? indentation error at line " + newlineToken.getLine()
                                + ": indentation does not match any open block level.");
            }
        }
    }

    private void handleEndOfFile(Token eofToken) {
        if (!eofHandled) {
            eofHandled = true;
            while (indentStack.peek() != 0) {
                indentStack.pop();
                pendingTokens.add(syntheticToken(YLexer.DEDENT, "<DEDENT>", eofToken));
            }
        }
        pendingTokens.add(eofToken);
    }

    private int widthOf(Token newlineToken) {
        String text = newlineToken.getText();
        int breakIndex = Math.max(text.lastIndexOf('\n'), text.lastIndexOf('\r'));
        return text.length() - (breakIndex + 1);
    }

    private boolean isBlankOrCommentOnlyAhead() {
        CharStream input = getInputStream();
        int first = input.LA(1);
        int second = input.LA(2);

        if (first == '\r' || first == '\n') {
            return true;
        }
        if (first == '/' && (second == '/' || second == '*')) {
            return true;
        }
        return false;
    }

    private Token syntheticToken(int type, String text, Token basedOn) {
        CommonToken token = new CommonToken(basedOn);
        token.setType(type);
        token.setText(text);
        token.setChannel(Token.DEFAULT_CHANNEL);
        return token;
    }
}