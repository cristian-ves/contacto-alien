package com.usac.contactoalien.contactoalienbackend.codegen.gen;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.Operand;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadBuilder;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.QuadOp;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.function.Supplier;

/**
Language-independent lowering of operators: the three front ends map their syntax trees
to calls on this class, which picks the quadruples from the static types of the operands.
Integers, characters and booleans are plain words; strings are concatenated at run time.
A decimal operand promotes the other operand of a binary operator to decimal.
*/
public final class Operations {

    // How a null reference is written when it is printed or concatenated.
    private static final String NULL_TEXT = "null";

    private final QuadBuilder quads;
    private final BooleanText booleans;
    private final BranchingOperations branching;
    private final InputOperations input;
    private ObjectFormatter objectFormatter;

    public Operations(QuadBuilder quads) {
        this(quads, BooleanText.LATIN);
    }

    // booleans selects the words used when a boolean is printed or concatenated.
    public Operations(QuadBuilder quads, BooleanText booleans) {
        this.quads = quads;
        this.booleans = booleans;
        this.branching = new BranchingOperations(quads, this);
        this.input = new InputOperations(quads, booleans);
    }

    // The front ends that can print reference values (Y? structures) install their formatter here.
    public void setObjectFormatter(ObjectFormatter formatter) {
        this.objectFormatter = formatter;
    }

    public ExprValue binary(String symbol, ExprValue left, ExprValue right, ParserRuleContext at) {
        return switch (symbol) {
            case "+" -> left.type().kind() == GenType.Kind.STRING || right.type().kind() == GenType.Kind.STRING
                    ? concat(left, right, at)
                    : arithmetic(QuadOp.ADD, left, right, at);
            case "-" -> arithmetic(QuadOp.SUB, left, right, at);
            case "*" -> arithmetic(QuadOp.MUL, left, right, at);
            case "/" -> arithmetic(QuadOp.DIV, left, right, at);
            case "%" -> arithmetic(QuadOp.MOD, left, right, at);
            case "<" -> relational(QuadOp.LT, left, right, at);
            case ">" -> relational(QuadOp.GT, left, right, at);
            case "<=" -> relational(QuadOp.LE, left, right, at);
            case ">=" -> relational(QuadOp.GE, left, right, at);
            case "==" -> equality(QuadOp.EQ, left, right, at);
            case "!=" -> equality(QuadOp.NE, left, right, at);
            default -> throw GenerationException.unsupported(GenerationMessages.FEATURE_EXPRESSION, at);
        };
    }

    public ExprValue negate(ExprValue value, ParserRuleContext at) {
        requireNumeric(value, at);
        if (value.type().isFloat()) {
            return new ExprValue(quads.emitTemp(QuadOp.FNEG, value.operand(), null), GenType.FLOAT);
        }
        return new ExprValue(quads.emitTemp(QuadOp.NEG, value.operand(), null), GenType.INT);
    }

    /**
    Converts an integer or character to decimal when the destination (variable, parameter,
    return type...) is decimal; any other combination is returned unchanged.
    */
    public ExprValue coerce(ExprValue value, GenType target) {
        boolean widen = target != null && target.isFloat() && value.type().isNumeric() && !value.type().isFloat();
        if (!widen) {
            return value;
        }
        if (Operand.isIntLiteral(value.operand())) {
            return new ExprValue(Operand.floatLiteral(Long.parseLong(value.operand())), GenType.FLOAT);
        }
        return new ExprValue(quads.emitTemp(QuadOp.I2F, value.operand(), null), GenType.FLOAT);
    }

    // Drops the fractional part when a decimal is stored in an integer destination (x += 1.5 on an int x).
    public ExprValue truncate(ExprValue value, GenType target) {
        boolean narrow = value.type().isFloat() && target != null
                && (target.kind() == GenType.Kind.INT || target.kind() == GenType.Kind.CHAR);
        return narrow ? new ExprValue(quads.emitTemp(QuadOp.F2I, value.operand(), null), GenType.INT) : value;
    }

    public ExprValue not(ExprValue value) {
        return new ExprValue(quads.emitTemp(QuadOp.NOT, value.operand(), null), GenType.BOOL);
    }

    private String booleanText(ExprValue value) {
        String text = quads.temp();
        String end = quads.newLabel();
        quads.assign(text, Operand.stringLiteral(booleans.falseText()));
        quads.jumpIfFalse(value.operand(), end);
        quads.assign(text, Operand.stringLiteral(booleans.trueText()));
        quads.mark(end);
        return text;
    }

    // Converts any printable value to a string record so it can be concatenated.
    public ExprValue toStringValue(ExprValue value, ParserRuleContext at) {
        return switch (value.type().kind()) {
            case STRING -> value;
            case INT -> new ExprValue(quads.emitTemp(QuadOp.INT_TO_STR, value.operand(), null), GenType.STRING);
            case CHAR -> new ExprValue(quads.emitTemp(QuadOp.CHAR_TO_STR, value.operand(), null), GenType.STRING);
            case FLOAT -> new ExprValue(quads.emitTemp(QuadOp.FLOAT_TO_STR, value.operand(), null), GenType.STRING);
            case BOOL -> booleans.fromRuntime()
                    ? new ExprValue(quads.emitTemp(QuadOp.BOOL_TO_STR, value.operand(), null), GenType.STRING)
                    : new ExprValue(booleanText(value), GenType.STRING);
            case OBJECT -> formatObject(value, at);
            case NULL -> new ExprValue(Operand.stringLiteral(NULL_TEXT), GenType.STRING);
            default -> throw GenerationException.unsupported(GenerationMessages.FEATURE_OBJECT_PRINT, at);
        };
    }

    private ExprValue formatObject(ExprValue value, ParserRuleContext at) {
        if (objectFormatter == null) {
            throw GenerationException.unsupported(GenerationMessages.FEATURE_OBJECT_PRINT, at);
        }
        return objectFormatter.format(value, at);
    }

    public void print(ExprValue value, ParserRuleContext at) {
        if (!booleans.fromRuntime() && value.type().kind() == GenType.Kind.BOOL) {
            quads.emit(QuadOp.PRINT_STR, booleanText(value), null, null);
            return;
        }
        if (value.type().kind() == GenType.Kind.NULL) {
            quads.emit(QuadOp.PRINT_STR, Operand.stringLiteral(NULL_TEXT), null, null);
            return;
        }
        QuadOp op = switch (value.type().kind()) {
            case STRING -> QuadOp.PRINT_STR;
            case INT -> QuadOp.PRINT_INT;
            case CHAR -> QuadOp.PRINT_CHAR;
            case FLOAT -> QuadOp.PRINT_FLOAT;
            case BOOL -> QuadOp.PRINT_BOOL;
            case OBJECT -> {
                quads.emit(QuadOp.PRINT_STR, formatObject(value, at).operand(), null, null);
                yield null;
            }
            default -> throw GenerationException.unsupported(GenerationMessages.FEATURE_OBJECT_PRINT, at);
        };
        if (op != null) {
            quads.emit(op, value.operand(), null, null);
        }
    }

    // Reads one line of standard input as a value of the given type (integer when the type is not readable);
    // a bool is true when the line is exactly "verum".
    public boolean isReadable(GenType type) {
        return input.isReadable(type);
    }

    public ExprValue read(GenType type) {
        return input.read(type);
    }

    public void printNewline() {
        quads.emit(QuadOp.PRINT_NL, null, null, null);
    }

    // Short-circuit && and ||: the right operand is only evaluated when the left one does not decide.
    public ExprValue logical(boolean isAnd, Supplier<ExprValue> left, Supplier<ExprValue> right) {
        return branching.logical(isAnd, left, right);
    }

    // condition ? whenTrue : whenFalse
    public ExprValue conditional(Supplier<ExprValue> condition, Supplier<ExprValue> whenTrue,
                                 Supplier<ExprValue> whenFalse) {
        return branching.conditional(condition, whenTrue, whenFalse);
    }

    private ExprValue concat(ExprValue left, ExprValue right, ParserRuleContext at) {
        String a = toStringValue(left, at).operand();
        String b = toStringValue(right, at).operand();
        return new ExprValue(quads.emitTemp(QuadOp.STR_CONCAT, a, b), GenType.STRING);
    }

    private ExprValue arithmetic(QuadOp op, ExprValue left, ExprValue right, ParserRuleContext at) {
        requireNumeric(left, at);
        requireNumeric(right, at);
        if (left.type().isFloat() || right.type().isFloat()) {
            QuadOp decimalOp = decimalVersion(op, at);
            return new ExprValue(quads.emitTemp(decimalOp, coerce(left, GenType.FLOAT).operand(),
                    coerce(right, GenType.FLOAT).operand()), GenType.FLOAT);
        }
        return new ExprValue(quads.emitTemp(op, left.operand(), right.operand()), GenType.INT);
    }

    private ExprValue relational(QuadOp op, ExprValue left, ExprValue right, ParserRuleContext at) {
        requireNumeric(left, at);
        requireNumeric(right, at);
        return compare(op, left, right, at);
    }

    private ExprValue equality(QuadOp op, ExprValue left, ExprValue right, ParserRuleContext at) {
        boolean bothStrings = left.type().kind() == GenType.Kind.STRING && right.type().kind() == GenType.Kind.STRING;
        if (bothStrings) {
            String same = quads.emitTemp(QuadOp.STR_EQ, left.operand(), right.operand());
            return op == QuadOp.EQ ? new ExprValue(same, GenType.BOOL) : not(new ExprValue(same, GenType.BOOL));
        }
        if (left.type().isNumeric() && right.type().isNumeric()) {
            return compare(op, left, right, at);
        }
        return new ExprValue(quads.emitTemp(op, left.operand(), right.operand()), GenType.BOOL);
    }

    private ExprValue compare(QuadOp op, ExprValue left, ExprValue right, ParserRuleContext at) {
        if (left.type().isFloat() || right.type().isFloat()) {
            return new ExprValue(quads.emitTemp(decimalVersion(op, at), coerce(left, GenType.FLOAT).operand(),
                    coerce(right, GenType.FLOAT).operand()), GenType.BOOL);
        }
        return new ExprValue(quads.emitTemp(op, left.operand(), right.operand()), GenType.BOOL);
    }

    private QuadOp decimalVersion(QuadOp op, ParserRuleContext at) {
        return switch (op) {
            case ADD -> QuadOp.FADD;
            case SUB -> QuadOp.FSUB;
            case MUL -> QuadOp.FMUL;
            case DIV -> QuadOp.FDIV;
            case MOD -> QuadOp.FMOD;
            case LT -> QuadOp.FLT;
            case GT -> QuadOp.FGT;
            case LE -> QuadOp.FLE;
            case GE -> QuadOp.FGE;
            case EQ -> QuadOp.FEQ;
            case NE -> QuadOp.FNE;
            default -> throw GenerationException.unsupported(GenerationMessages.FEATURE_EXPRESSION, at);
        };
    }

    private void requireNumeric(ExprValue value, ParserRuleContext at) {
        if (!value.type().isNumeric()) {
            throw GenerationException.unsupported(GenerationMessages.FEATURE_EXPRESSION, at);
        }
    }
}
