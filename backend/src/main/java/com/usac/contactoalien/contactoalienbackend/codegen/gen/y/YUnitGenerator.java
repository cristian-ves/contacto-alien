package com.usac.contactoalien.contactoalienbackend.codegen.gen.y;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.BooleanText;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LocalScope;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.FunctionInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ProgramIndex;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.grammar.y.YParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/**
Translates the functions of one .y file. Each function becomes a labelled routine whose
parameters arrive on the stack in declaration order; read their signatures with signatures()
before translating any caller.
*/
public final class YUnitGenerator {

    private static final String LABEL_PREFIX = "y_";
    private static final int STRUCT_ID = 0;

    private final YParser.ProgramContext tree;
    private final QuadBuilder quads;
    private final ProgramIndex index;

    public YUnitGenerator(YParser.ProgramContext tree, QuadBuilder quads, ProgramIndex index) {
        this.tree = tree;
        this.quads = quads;
        this.index = index;
    }

    // Registers every structure and function of the file so references resolve regardless of order.
    public static void register(YParser.ProgramContext tree, ProgramIndex index) {
        registerStructs(tree, index);
        for (YParser.FunctionDeclarationContext function : tree.functionsSection().functionDeclaration()) {
            List<GenType> params = parameters(function).stream().map(parameter -> parameter(parameter).type()).toList();
            GenType returnType = function.type() == null ? GenType.VOID : YTypeMapper.map(function.type());
            String name = function.IDENTIFIER().getText();
            index.addFunction(new FunctionInfo(name, LABEL_PREFIX + name, params, returnType));
        }
    }

    // A structure declared inside another structure or inside a function is one more structure with its own name,
    // as the analyzer sees it, so every declaration in the tree is registered.
    private static void registerStructs(ParseTree node, ProgramIndex index) {
        if (node instanceof YParser.StructDeclarationContext struct) {
            registerStruct(struct, index);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            registerStructs(node.getChild(i), index);
        }
    }

    // A structure is laid out like an object without methods; array fields remember their declared length.
    private static void registerStruct(YParser.StructDeclarationContext struct, ProgramIndex index) {
        ClassInfo info = new ClassInfo(struct.IDENTIFIER().getText(), STRUCT_ID);
        for (YParser.StructMemberContext member : struct.structMember()) {
            if (member.variableDeclaration() == null) {
                continue;
            }
            YParser.VariableDeclarationContext field = member.variableDeclaration();
            GenType element = YTypeMapper.map(field.type());
            if (field.LBRACK().isEmpty()) {
                info.addField(field.IDENTIFIER().getText(), element);
            } else {
                info.addField(field.IDENTIFIER().getText(), GenType.array(element), Integer.parseInt(field.INT_LITERAL(0).getText()));
            }
        }
        index.addClass(info);
    }

    public void generate() {
        for (YParser.FunctionDeclarationContext function : tree.functionsSection().functionDeclaration()) {
            function(function);
        }
    }

    private void function(YParser.FunctionDeclarationContext ctx) {
        GenContext gen = GenContext.create(quads, index, BooleanText.SPANISH);
        YExpressionGen expressions = new YExpressionGen(gen);
        String label = LABEL_PREFIX + ctx.IDENTIFIER().getText();
        quads.emit(QuadOp.FUNC_BEGIN, null, null, label);
        gen.scope().push();
        for (YParser.ParameterContext parameter : parameters(ctx)) {
            Param param = parameter(parameter);
            LocalScope.Variable variable = gen.scope().declareLocal(param.name(), param.type());
            quads.emit(QuadOp.RECV, null, null, variable.operand());
        }
        GenType returnType = ctx.type() == null ? GenType.VOID : YTypeMapper.map(ctx.type());
        new YStatementGen(gen, expressions, returnType).block(ctx.statement());
        gen.scope().pop();
        quads.emit(QuadOp.FUNC_END, null, null, label);
    }

    private static List<YParser.ParameterContext> parameters(YParser.FunctionDeclarationContext function) {
        return function.parameterList() == null ? List.of() : function.parameterList().parameter();
    }

    private record Param(String name, GenType type) {
    }

    // Arrays and structures are passed by reference: the callee receives the address of the heap value.
    private static Param parameter(YParser.ParameterContext parameter) {
        return switch (parameter) {
            case YParser.ScalarParameterContext p -> new Param(p.IDENTIFIER().getText(), YTypeMapper.map(p.type()));
            case YParser.ArrayParameterContext p ->
                    new Param(p.IDENTIFIER().getText(), arrayOf(YTypeMapper.map(p.type()), p.LBRACK().size()));
            case YParser.StructParameterContext p ->
                    new Param(p.IDENTIFIER(1).getText(), GenType.object(p.IDENTIFIER(0).getText()));
            default -> throw GenerationException.unsupported(GenerationMessages.FEATURE_EXPRESSION, parameter);
        };
    }

    private static GenType arrayOf(GenType element, int dimensions) {
        GenType type = element;
        for (int i = 0; i < dimensions; i++) {
            type = GenType.array(type);
        }
        return type;
    }
}
