package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import org.antlr.v4.runtime.ParserRuleContext;

/**
Turns a reference value (a Y? structure) into its text so it can be printed or concatenated.
*/
public interface ObjectFormatter {

    ExprValue format(ExprValue value, ParserRuleContext at);
}
