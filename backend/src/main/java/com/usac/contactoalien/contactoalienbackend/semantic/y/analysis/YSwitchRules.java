package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.ExpressionContext;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.PostfixContext;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.PrimaryContext;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser.UnaryContext;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.HashSet;
import java.util.Set;

/**
Rules of one `elegir`: the subject is an entero, caracter or cadena, every `caso` is a literal
of a comparable type, and no literal repeats. Create one instance per statement.
*/
final class YSwitchRules {

    private final YErrorReporter errors;
    private final Set<String> seen = new HashSet<>();

    YSwitchRules(YErrorReporter errors) {
        this.errors = errors;
    }

    void checkSubject(YType subject, ParserRuleContext at) {
        if (!subject.isUnknown() && (subject.isArray() || !isSwitchable(subject))) {
            errors.report("'elegir' subject must be entero, caracter or cadena, found '" + subject + "'", at);
        }
    }

    void checkCase(YType subject, YType value, ExpressionContext at) {
        String key = literalKey(at);
        if (key == null) {
            errors.report("'caso' value must be a literal", at);
        } else if (!seen.add(key)) {
            errors.report("Duplicate 'caso' value " + at.getText(), at);
        }
        if (!subject.isUnknown() && !value.isUnknown() && !comparable(subject, value)) {
            errors.report("'caso' value of type '" + value + "' cannot be compared with the 'elegir' subject of type '"
                    + subject + "'", at);
        }
    }

    private boolean isSwitchable(YType type) {
        return type.getKind() == YType.Kind.ENTERO || type.getKind() == YType.Kind.CARACTER
                || type.getKind() == YType.Kind.CADENA;
    }

    private boolean comparable(YType subject, YType value) {
        if (subject.isArray() || value.isArray() || !isSwitchable(value)) {
            return false;
        }
        boolean text = subject.getKind() == YType.Kind.CADENA;
        return text == (value.getKind() == YType.Kind.CADENA);
    }

    // Identity of a numeric, character or string literal (a leading minus is allowed), null for anything else.
    private String literalKey(ExpressionContext value) {
        UnaryContext unary = YPlaceRules.descend(value, UnaryContext.class);
        String sign = "";
        if (unary != null && unary.MINUS() != null) {
            sign = "-";
            unary = unary.unary();
        }
        PostfixContext postfix = unary == null ? null : unary.postfix();
        if (postfix == null || !postfix.postfixOp().isEmpty()) {
            return null;
        }
        PrimaryContext primary = postfix.primary();
        if (primary.INT_LITERAL() != null) {
            return "n" + sign + primary.INT_LITERAL().getText();
        }
        if (primary.CHAR_LITERAL() != null && sign.isEmpty()) {
            return "c" + primary.CHAR_LITERAL().getText();
        }
        if (primary.FLOAT_LITERAL() != null) {
            return "f" + sign + primary.FLOAT_LITERAL().getText();
        }
        return primary.STRING_LITERAL() != null && sign.isEmpty() ? "s" + primary.STRING_LITERAL().getText() : null;
    }
}
