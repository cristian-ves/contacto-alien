package com.usac.contactoalien.contactoalienbackend.semantic.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.SwitchLabelContext;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser.SwitchStatementContext;
import com.usac.contactoalien.contactoalienbackend.semantic.errors.ErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.types.ZType;

import java.util.HashSet;
import java.util.Set;

/**
Rules for a switch: the selector is an int, char or String; every case constant has the
selector's type; no constant and no default appears twice.
*/
final class SwitchChecker {

    private final ErrorReporter errors;

    SwitchChecker(ErrorReporter errors) {
        this.errors = errors;
    }

    void check(ZType selector, SwitchStatementContext ctx) {
        boolean validSelector = selector.isUnknown() || (!selector.isArray() && (selector.getKind() == ZType.Kind.INT
                || selector.getKind() == ZType.Kind.CHAR || selector.getKind() == ZType.Kind.STRING));
        if (!validSelector) {
            errors.report("Switch expression must be int, char or String, found '" + selector + "'", ctx.expression());
        }
        Set<String> seen = new HashSet<>();
        boolean hasDefault = false;
        for (SwitchLabelContext label : ctx.switchLabel()) {
            if (label.DEFAULT() != null) {
                if (hasDefault) {
                    errors.report("Duplicate 'default' label in switch", label);
                }
                hasDefault = true;
                continue;
            }
            if (label.INT_LITERAL() != null && !IntegerLiterals.fits(label.INT_LITERAL().getText())) {
                errors.report(IntegerLiterals.outOfRange(label.INT_LITERAL().getText()), label);
            }
            ZType labelType = typeOf(label);
            if (validSelector && !selector.isUnknown() && !labelType.equals(selector)) {
                errors.report("Case label of type '" + labelType + "' does not match the switch expression of type '"
                        + selector + "'", label);
            } else if (!seen.add(label.getChild(1).getText())) {
                errors.report("Duplicate case label " + label.getChild(1).getText(), label);
            }
        }
    }

    private static ZType typeOf(SwitchLabelContext label) {
        if (label.INT_LITERAL() != null) {
            return ZType.INT;
        }
        if (label.CHAR_LITERAL() != null) {
            return ZType.CHAR;
        }
        return label.STRING_LITERAL() != null ? ZType.STRING : ZType.BOOLEAN;
    }
}
