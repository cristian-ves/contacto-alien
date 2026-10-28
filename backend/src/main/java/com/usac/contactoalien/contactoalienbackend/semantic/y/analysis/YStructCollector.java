package com.usac.contactoalien.contactoalienbackend.semantic.y.analysis;

import com.usac.contactoalien.contactoalienbackend.semantic.types.IntegerLiterals;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParserBaseVisitor;
import com.usac.contactoalien.contactoalienbackend.semantic.y.errors.YErrorReporter;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructRegistry;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YStructSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.symbols.YVariableSymbol;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YType;
import com.usac.contactoalien.contactoalienbackend.semantic.y.types.YTypeResolver;

// Two-pass collection of every structure declared anywhere in the program (top-level, nested
// inside another structure, or declared locally as a statement inside a function). Pass 1
// registers every structure's bare name; pass 2 resolves each structure's fields, so
// structures may reference each other regardless of declaration order (including mutual
// references).
public final class YStructCollector extends YParserBaseVisitor<Void> {

    private final YStructRegistry registry;
    private final YErrorReporter errors;
    private boolean fieldsPass;

    public YStructCollector(YStructRegistry registry, YErrorReporter errors) {
        this.registry = registry;
        this.errors = errors;
    }

    public void collect(YParser.ProgramContext programCtx) {
        fieldsPass = false;
        visit(programCtx);
        fieldsPass = true;
        visit(programCtx);
    }

    @Override
    public Void visitStructDeclaration(YParser.StructDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        if (!fieldsPass) {
            if (!registry.declare(name)) {
                errors.report("Structure '" + name + "' is already declared", ctx);
            }
        } else {
            YStructSymbol struct = registry.resolve(name);
            for (YParser.StructMemberContext member : ctx.structMember()) {
                if (member.variableDeclaration() != null) {
                    declareField(struct, member.variableDeclaration());
                }
            }
        }
        return visitChildren(ctx); // keep descending to reach any nested structure declarations
    }

    private void declareField(YStructSymbol struct, YParser.VariableDeclarationContext ctx) {
        YType base = YTypeResolver.resolve(ctx.type(), registry, errors);
        YType type = ctx.LBRACK().isEmpty() ? base : base.asArray(1);
        String name = ctx.IDENTIFIER().getText();
        for (org.antlr.v4.runtime.tree.TerminalNode size : ctx.INT_LITERAL()) {
            if (IntegerLiterals.isLengthTooLarge(size.getText()) || IntegerLiterals.length(size.getText()) < 1) {
                errors.report("Array size of field '" + name + "' must be between 1 and " + Integer.MAX_VALUE, ctx);
            }
        }
        if (ctx.LBRACK().size() > 1) {
            errors.report("Field '" + name + "' of structure '" + struct.getName() + "' can only have one array dimension", ctx);
        }
        if (!struct.declareField(new YVariableSymbol(name, type))) {
            errors.report("Field '" + name + "' is already declared in structure '" + struct.getName() + "'", ctx);
        }
        // any initializer expression on a structure-member declaration is intentionally not
        // type-checked here: a structure definition only describes shape, not runtime values
    }
}