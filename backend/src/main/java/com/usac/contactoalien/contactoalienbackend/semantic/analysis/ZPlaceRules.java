package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.PostfixExpressionContext;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.PostfixOpContext;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.PrimaryExpressionContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
Which expressions name a place that can be assigned or incremented: a variable, a field or an
array element. Anything else (a literal, a call, a sum, a parenthesized name) is rejected by the
analyzer, so the code generator never meets a target it cannot write to.
*/
final class ZPlaceRules {

    private ZPlaceRules() {
    }

    // The postfix expression at the bottom of a chain of single-child precedence levels, or null.
    static PostfixExpressionContext postfixOf(ParserRuleContext ctx) {
        ParseTree node = ctx;
        while (node instanceof ParserRuleContext rule) {
            if (rule instanceof PostfixExpressionContext postfix) {
                return postfix;
            }
            if (rule.getChildCount() != 1) {
                return null;
            }
            node = rule.getChild(0);
        }
        return null;
    }

    static boolean isPlace(ParserRuleContext expression) {
        PostfixExpressionContext postfix = postfixOf(expression);
        return postfix != null && isPlace(postfix.primaryExpression(), postfix.postfixOp(), postfix.postfixOp().size());
    }

    // True when the postfix operator `op` (a ++ or --) is applied to a place.
    static boolean isPlaceBefore(PostfixOpContext op) {
        PostfixExpressionContext postfix = (PostfixExpressionContext) op.getParent();
        return isPlace(postfix.primaryExpression(), postfix.postfixOp(), postfix.postfixOp().indexOf(op));
    }

    // True when the primary followed by the first count operators names a place: a name, a field or an element.
    private static boolean isPlace(PrimaryExpressionContext primary, List<PostfixOpContext> ops, int count) {
        if (count == 0) {
            return primary.IDENTIFIER() != null && primary.NEW() == null;
        }
        PostfixOpContext last = ops.get(count - 1);
        boolean isCallName = last.DOT() != null && count < ops.size() && ops.get(count).LPAREN() != null;
        return (last.DOT() != null && !isCallName) || last.LBRACKET() != null;
    }
}
