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
Base class for the ANTLR-generated YLexer (see "superClass" in YLexer.g4).
Owns all indentation-sensitive logic: synthesizing INDENT/DEDENT from
NEWLINE tokens, and suppressing that tracking entirely while inside an
explicit '{' '}' block (only "elegir" uses braces in Y?), the same way
Python suppresses indentation inside (), [], {}.

ALGORITHM (indentation)
NEWLINE matches the line break plus the spaces opening the next line, so
its width is known immediately. A stack of open indentation widths
(starting at 0) is compared against that width: wider pushes and emits
INDENT, narrower pops and emits one DEDENT per level until an exact match
is found (or an indentation error is raised), equal emits nothing. EOF
flushes any remaining open levels as DEDENTs. Blank and comment-only
lines never affect the stack.

END OF FILE
The parser needs every logical line to end with NEWLINE. A line break
followed only by blanks and the end of the file closes the last logical
line without touching the indentation stack, and EOF adds the NEWLINE itself
when the last token was not one, so a file behaves the same with or without
its final line break.

BRACE SUPPRESSION
LBRACE/RBRACE are ordinary tokens already recognized by the grammar; this
class just counts them. While braceDepth > 0, NEWLINE is still emitted
(YParser.g4 needs it as a statement separator inside "elegir"), but the
indentation stack is never touched and no INDENT/DEDENT is produced,
regardless of how the code inside the braces is visually indented.
*/
public abstract class YIndentLexerBase extends Lexer {

    private static final int TAB_WIDTH = 4;

    private final Deque<Integer> indentStack = new ArrayDeque<>();
    private final Queue<Token> pendingTokens = new LinkedList<>();
    private boolean hasSeenFirstStatement = false;
    private boolean eofHandled = false;
    private int lastEmittedType = Token.INVALID_TYPE;

    // Depth of currently open explicit '{' '}' blocks.
    private int braceDepth = 0;

    protected YIndentLexerBase(CharStream input) {
        super(input);
        indentStack.push(0);
    }

    @Override
    public Token nextToken() {
        Token token = produceToken();
        if (token.getChannel() == Token.DEFAULT_CHANNEL) {
            lastEmittedType = token.getType();
        }
        return token;
    }

    private Token produceToken() {
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
            return pendingTokens.isEmpty() ? produceToken() : pendingTokens.poll();
        }

        hasSeenFirstStatement = true;
        return next;
    }

    private void handleNewline(Token newlineToken) {
        if (getInputStream().LA(1) == Token.EOF) {
            // Only blanks follow: the line break closes the last logical line and changes no indentation.
            if (hasSeenFirstStatement && lastEmittedType != YLexer.NEWLINE) {
                pendingTokens.add(newlineToken);
            }
            return;
        }
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
                // The offending line starts right after the line break held by the NEWLINE token.
                // Recovering at the enclosing level keeps INDENT/DEDENT balanced.
                getErrorListenerDispatch().syntaxError(this, null, newlineToken.getLine() + 1, width,
                        "Indentation does not match any open block level", null);
            }
        }
    }

    private void handleEndOfFile(Token eofToken) {
        if (!eofHandled) {
            eofHandled = true;
            if (hasSeenFirstStatement && lastEmittedType != YLexer.NEWLINE && lastEmittedType != YLexer.DEDENT) {
                pendingTokens.add(syntheticToken(YLexer.NEWLINE, "\n", eofToken));
            }
            while (indentStack.peek() != 0) {
                indentStack.pop();
                pendingTokens.add(syntheticToken(YLexer.DEDENT, "<DEDENT>", eofToken));
            }
        }
        pendingTokens.add(eofToken);
    }

    // A tab counts as TAB_WIDTH columns so files indented with tabs open the same blocks as spaces would.
    private int widthOf(Token newlineToken) {
        String text = newlineToken.getText();
        int breakIndex = Math.max(text.lastIndexOf('\n'), text.lastIndexOf('\r'));
        int width = 0;
        for (int i = breakIndex + 1; i < text.length(); i++) {
            width += text.charAt(i) == '\t' ? TAB_WIDTH : 1;
        }
        return width;
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