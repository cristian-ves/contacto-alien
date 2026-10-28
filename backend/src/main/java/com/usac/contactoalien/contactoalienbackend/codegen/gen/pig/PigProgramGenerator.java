package com.usac.contactoalien.contactoalienbackend.codegen.gen.pig;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ProgramIndex;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;

/**
Translates a whole Pig Latin program into one function, ENTRY_LABEL: the globals declared in
VARIABILES> are initialized first, then the statements of MAIOR> run in order. The runtime
entry point calls this function and exits when it returns.
*/
public final class PigProgramGenerator {

    public static final String ENTRY_LABEL = "pig_main";

    private final PigParser.ProgramContext tree;
    private final QuadBuilder quads;
    private final ProgramIndex index;

    public PigProgramGenerator(PigParser.ProgramContext tree, QuadBuilder quads, ProgramIndex index) {
        this.tree = tree;
        this.quads = quads;
        this.index = index;
    }

    public void generate() {
        GenContext gen = GenContext.create(quads, index);
        PigExpressionGen expressions = new PigExpressionGen(gen);
        quads.emit(QuadOp.FUNC_BEGIN, null, null, ENTRY_LABEL);
        gen.scope().push();
        if (tree.variabilesSection() != null) {
            new PigDeclarationGen(gen, expressions).section(tree.variabilesSection());
        }
        new PigStatementGen(gen, expressions).statements(tree.maiorSection().statement());
        gen.scope().pop();
        quads.emit(QuadOp.FUNC_END, null, null, ENTRY_LABEL);
    }
}
