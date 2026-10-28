package com.usac.contactoalien.contactoalienbackend.grammar;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import org.antlr.v4.runtime.tree.ParseTree;

/**
Recognizes the one place where `readln()` may feed a non-String variable: an initializer or
an assignment whose whole right side is the bare call. The line read is then converted to the
type of the variable, like `<<` does in Pig Latin.
*/
public final class ReadlnForms {

    private ReadlnForms() {
    }

    public static boolean isBareCall(ParseTree expression) {
        ParseTree node = expression;
        while (node != null && node.getChildCount() == 1) {
            node = node.getChild(0);
        }
        return node instanceof ZetarianoParser.PrimaryExpressionContext primary && primary.READLN() != null;
    }
}
