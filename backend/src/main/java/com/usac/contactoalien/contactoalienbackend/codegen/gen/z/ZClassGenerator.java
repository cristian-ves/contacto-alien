package com.usac.contactoalien.contactoalienbackend.codegen.gen.z;

import com.usac.contactoalien.contactoalienbackend.codegen.gen.BooleanText;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenContext;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenType;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.LocalScope;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ClassInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.MethodInfo;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.index.ProgramIndex;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import com.usac.contactoalien.contactoalienbackend.semantic.analysis.SuperCallShape;
import com.usac.contactoalien.contactoalienbackend.grammar.zetariano.ZetarianoParser;

import java.util.ArrayList;
import java.util.List;

/**
Translates one Zetariano class. Every method and constructor becomes a routine whose first
argument is the object (`this`) followed by the declared parameters. Labels are
Class__name__signature so overloads stay apart; constructors use the name "new".
*/
public final class ZClassGenerator {

    private static final String CONSTRUCTOR_NAME = "new";

    private final ZetarianoParser.ClassDeclarationContext tree;
    private final QuadBuilder quads;
    private final ProgramIndex index;

    public ZClassGenerator(ZetarianoParser.ClassDeclarationContext tree, QuadBuilder quads, ProgramIndex index) {
        this.tree = tree;
        this.quads = quads;
        this.index = index;
    }

    // Lays out the object (fields) and records member signatures so classes can refer to each other.
    public static void register(ZetarianoParser.ClassDeclarationContext tree, ProgramIndex index, int classId) {
        String className = tree.IDENTIFIER(0).getText();
        ClassInfo info = new ClassInfo(className, classId, index.classInfo(superNameOf(tree)));
        for (ZetarianoParser.ClassMemberContext member : tree.classMember()) {
            if (member.fieldDeclaration() != null) {
                info.addField(member.fieldDeclaration().IDENTIFIER().getText(),
                        ZTypeMapper.map(member.fieldDeclaration().type()));
            } else if (member.constructorDeclaration() != null) {
                info.addConstructor(signature(className, CONSTRUCTOR_NAME, GenType.VOID,
                        member.constructorDeclaration().parameterList()));
            } else {
                ZetarianoParser.MethodDeclarationContext method = member.methodDeclaration();
                MethodInfo signature = signature(className, method.IDENTIFIER().getText(),
                        ZTypeMapper.mapReturn(method.type()), method.parameterList());
                boolean isPrivate = method.visibilityModifier() != null && method.visibilityModifier().PRIVATE() != null;
                info.addMethod(new MethodInfo(signature.owner(), signature.name(), signature.label(), signature.params(),
                        signature.returnType(), isPrivate));
            }
        }
        index.addClass(info);
    }

    // Name of the class this one extends, or null. The superclass must be registered before the class.
    public static String superNameOf(ZetarianoParser.ClassDeclarationContext tree) {
        return tree.EXTENDS() == null ? null : tree.IDENTIFIER(1).getText();
    }

    /**
    Declares the method table of every class with dispatchable methods: one VTABLE quadruple per slot,
    naming the routine that runs for that slot on an object of the class.
    */
    public static void declareMethodTables(ProgramIndex index, QuadBuilder quads) {
        for (ClassInfo info : index.classes()) {
            List<MethodInfo> table = info.vtable();
            for (int slot = 0; slot < table.size(); slot++) {
                quads.emit(QuadOp.VTABLE, Integer.toString(info.id()), Integer.toString(slot), table.get(slot).label());
            }
        }
    }

    public void generate() {
        ClassInfo info = index.classInfo(tree.IDENTIFIER(0).getText());
        for (ZetarianoParser.ClassMemberContext member : tree.classMember()) {
            if (member.constructorDeclaration() != null) {
                ZetarianoParser.ConstructorDeclarationContext ctor = member.constructorDeclaration();
                routine(info, CONSTRUCTOR_NAME, GenType.VOID, ctor.parameterList(), ctor.block());
            } else if (member.methodDeclaration() != null) {
                ZetarianoParser.MethodDeclarationContext method = member.methodDeclaration();
                routine(info, method.IDENTIFIER().getText(), ZTypeMapper.mapReturn(method.type()),
                        method.parameterList(), method.block());
            }
        }
    }

    private void routine(ClassInfo info, String name, GenType returnType,
                         ZetarianoParser.ParameterListContext parameters, ZetarianoParser.BlockContext body) {
        GenContext gen = GenContext.create(quads, index, BooleanText.JAVA);
        ZExpressionGen expressions = new ZExpressionGen(gen, info);
        String label = signature(info.name(), name, returnType, parameters).label();
        quads.emit(QuadOp.FUNC_BEGIN, null, null, label);
        gen.scope().push();
        declareParameter(gen, ZExpressionGen.THIS, GenType.object(info.name()));
        for (ZetarianoParser.ParameterContext parameter : parametersOf(parameters)) {
            declareParameter(gen, parameter.IDENTIFIER().getText(), ZTypeMapper.map(parameter.type()));
        }
        if (name.equals(CONSTRUCTOR_NAME) && !startsWithSuperCall(body)) {
            gen.calls().callImplicitSuper(info, gen.variable(ZExpressionGen.THIS, body));
        }
        new ZStatementGen(gen, expressions, returnType).block(body);
        gen.scope().pop();
        quads.emit(QuadOp.FUNC_END, null, null, label);
    }

    private static boolean startsWithSuperCall(ZetarianoParser.BlockContext body) {
        return !body.statement().isEmpty() && SuperCallShape.isSuperConstructorCall(body.statement(0));
    }

    private void declareParameter(GenContext gen, String name, GenType type) {
        LocalScope.Variable variable = gen.scope().declareLocal(name, type);
        quads.emit(QuadOp.RECV, null, null, variable.operand());
    }

    private static MethodInfo signature(String owner, String name, GenType returnType,
                                        ZetarianoParser.ParameterListContext parameters) {
        List<GenType> types = new ArrayList<>();
        StringBuilder label = new StringBuilder(owner).append("__").append(name).append("__");
        for (ZetarianoParser.ParameterContext parameter : parametersOf(parameters)) {
            GenType type = ZTypeMapper.map(parameter.type());
            types.add(type);
            label.append(type.signatureCode());
        }
        return new MethodInfo(owner, name, label.toString(), types, returnType);
    }

    private static List<ZetarianoParser.ParameterContext> parametersOf(ZetarianoParser.ParameterListContext list) {
        return list == null ? List.of() : list.parameter();
    }
}
