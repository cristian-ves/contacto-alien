package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.PostfixContext;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.PostfixOpContext;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.PrimaryContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
Which expressions name a place that can be assigned or incremented: a variable, a field of a
structure or an element of an array. Also finds the node at the bottom of a chain of
single-child rules, which is how the analyzers see through the precedence levels.
*/
final class YPlaceRules {

    private YPlaceRules() {
    }

    static <T extends ParserRuleContext> T descend(ParserRuleContext ctx, Class<T> type) {
        ParseTree node = ctx;
        while (node instanceof ParserRuleContext rule) {
            if (type.isInstance(rule)) {
                return type.cast(rule);
            }
            if (rule.getChildCount() != 1) {
                return null;
            }
            node = rule.getChild(0);
        }
        return null;
    }

    static boolean isPlace(PostfixContext postfix) {
        return isPlace(postfix.primary(), postfix.postfixOp(), postfix.postfixOp().size());
    }

    // True when the primary followed by the first count operators names a place.
    static boolean isPlace(PrimaryContext primary, List<PostfixOpContext> ops, int count) {
        if (count == 0) {
            return primary.IDENTIFIER() != null;
        }
        PostfixOpContext last = ops.get(count - 1);
        return last.DOT() != null || last.LBRACK() != null;
    }
}
