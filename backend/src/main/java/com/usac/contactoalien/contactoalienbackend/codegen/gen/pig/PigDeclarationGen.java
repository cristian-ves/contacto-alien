package com.usac.contactoalien.contactoalienbackend.codegen.gen.pig;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.ExprValue;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LocalScope;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.grammar.pig.PigParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;


// Translates the VARIABILES> section: every declaration (variable, series, structure, object) becomes a global initialized in program order.
final class PigDeclarationGen {

    private final GenContext gen;
    private final PigExpressionGen exprs;

    PigDeclarationGen(GenContext gen, PigExpressionGen exprs) {
        this.gen = gen;
        this.exprs = exprs;
    }

    void section(PigParser.VariabilesSectionContext section) {
        for (PigParser.VariableDeclContext declaration : section.variableDecl()) {
            if (declaration.basicVarDecl() != null) {
                basic(declaration.basicVarDecl());
            } else if (declaration.objectInstanceDecl() != null) {
                object(declaration.objectInstanceDecl());
            } else if (declaration.arrayDecl() != null) {
                array(declaration.arrayDecl());
            } else {
                struct(declaration.structInstanceDecl());
            }
        }
    }

    // `esto x : tipo valor;` or `esto x : valor;` (the type is then the type of the value).
    private void basic(PigParser.BasicVarDeclContext ctx) {
        GenType declared = ctx.tipoBasico() == null ? null : PigGenTypeMapper.map(ctx.tipoBasico());
        ExprValue value = ctx.expression() == null ? null : exprs.expression(ctx.expression());
        GenType type = declared != null ? declared : value.type();
        LocalScope.Variable variable = gen.scope().declareGlobal(ctx.IDENTIFIER().getText(), type);
        ExprValue initial = value != null ? value : new ExprValue(type.defaultOperand(), type);
        gen.members().variableTarget(new ExprValue(variable.operand(), type)).write(initial);
    }

    // `series a[n] : tipo {v0, v1...}`: the array is created with n elements and the values fill it from index 0.
    private void array(PigParser.ArrayDeclContext ctx) {
        GenType element = ctx.tipoBasico() != null ? PigGenTypeMapper.map(ctx.tipoBasico())
                : GenType.object(ctx.IDENTIFIER(1).getText());
        ExprValue array = gen.members().newArray(element, ctx.INT_LITERAL().getText());
        LocalScope.Variable variable = gen.scope().declareGlobal(ctx.IDENTIFIER(0).getText(), array.type());
        gen.quads().assign(variable.operand(), array.operand());
        for (int i = 0; i < ctx.expression().size(); i++) {
            ExprValue position = new ExprValue(Integer.toString(i), GenType.INT);
            gen.members().elementTarget(array, position, ctx).write(exprs.expression(ctx.expression(i)));
        }
    }

    // `esto s : Estructura {v0, v1...}`: values are assigned to the fields in declaration order.
    private void struct(PigParser.StructInstanceDeclContext ctx) {
        ClassInfo info = gen.index().classInfo(ctx.IDENTIFIER(1).getText());
        if (info == null) {
            throw new GenerationException(GenerationMessages.UNKNOWN_CLASS + ctx.IDENTIFIER(1).getText(), ctx);
        }
        ExprValue object = gen.members().structOf(info, ctx.expression().stream().map(exprs::expression).toList());
        LocalScope.Variable variable = gen.scope().declareGlobal(ctx.IDENTIFIER(0).getText(), object.type());
        gen.quads().assign(variable.operand(), object.operand());
    }

    // The variable takes the declared type after the closing colon when present (`novus Sub(..) : Base`,
    // `null : Base`); otherwise the type of the instantiated class.
    private void object(PigParser.ObjectInstanceDeclContext ctx) {
        List<TerminalNode> names = ctx.IDENTIFIER();
        ExprValue object;
        String declared;
        if (ctx.NULL() != null) {
            declared = names.get(1).getText();
            object = new ExprValue("0", GenType.object(declared));
        } else {
            object = exprs.instantiate(names.get(1).getText(), ctx.argumentList(), ctx);
            declared = names.size() > 2 ? names.get(2).getText() : null;
        }
        GenType type = declared != null ? GenType.object(declared) : object.type();
        LocalScope.Variable variable = gen.scope().declareGlobal(names.get(0).getText(), type);
        gen.quads().assign(variable.operand(), object.operand());
    }
}
