package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/**
Recognizes, from the shape of the syntax tree alone, a statement that is exactly `super(args);`.
The expression rules nest one inside the other, so the call is found by following single-child
rules down to the postfix expression.
*/
public final class SuperCallShape {

    private SuperCallShape() {
    }

    public static boolean isSuperConstructorCall(ZetarianoParser.StatementContext statement) {
        if (statement == null || statement.expressionStatement() == null) {
            return false;
        }
        ZetarianoParser.PostfixExpressionContext postfix = descend(statement.expressionStatement().expression());
        return postfix != null
                && postfix.primaryExpression().SUPER() != null
                && postfix.postfixOp().size() == 1
                && postfix.postfixOp(0).LPAREN() != null;
    }

    private static ZetarianoParser.PostfixExpressionContext descend(ParserRuleContext start) {
        ParseTree node = start;
        while (node instanceof ParserRuleContext rule) {
            if (rule instanceof ZetarianoParser.PostfixExpressionContext postfix) {
                return postfix;
            }
            if (rule.getChildCount() != 1) {
                return null;
            }
            node = rule.getChild(0);
        }
        return null;
    }
}
