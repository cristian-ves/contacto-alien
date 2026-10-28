package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ProgramIndex;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

/**
The helpers every function body needs while it is translated. Each function gets its own
context (own scope and loop stack) over the same quadruple builder and program index.
*/
public record GenContext(QuadBuilder quads, Operations ops, CallEmitter calls, ControlFlow flow,
                         LocalScope scope, ProgramIndex index, MemberAccess members) {

    public static GenContext create(QuadBuilder quads, ProgramIndex index) {
        return create(quads, index, BooleanText.LATIN);
    }

    // booleans: the words a boolean prints as (verum/falsus for Pig Latin, true/false for Zetariano, verdadero/falso for Y?).
    public static GenContext create(QuadBuilder quads, ProgramIndex index, BooleanText booleans) {
        Operations ops = new Operations(quads, booleans);
        MemberAccess members = new MemberAccess(quads, ops, index);
        ops.setObjectFormatter(new StructFormatter(quads, ops, index, members));
        return new GenContext(quads, ops, new CallEmitter(quads, ops, index), new ControlFlow(quads, ops),
                new LocalScope(quads.names()), index, members);
    }

    public ExprValue variable(String name, ParserRuleContext at) {
        LocalScope.Variable found = scope.lookup(name);
        if (found == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_VARIABLE + name, at);
        }
        return new ExprValue(found.operand(), found.type());
    }
}
