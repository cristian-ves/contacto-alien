package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

/**
Runtime routines that turn text into numbers: str_to_int and str_to_float. Both skip blanks and
a sign first and stop at the first character that cannot belong to the number.
Conventions are those of RuntimeLibrary.
*/
final class RuntimeParsing {

    private RuntimeParsing() {
    }

    static String text() {
        return PARSE.formatted(skipBlanksAndSign("si"), skipBlanksAndSign("sf"));
    }

    // Shared prologue of both parsers: t0 = cursor, t1 = bytes left, t5 = 1 when the number is negative.
    private static String skipBlanksAndSign(String prefix) {
        return """
                li t5, 0
            %1$s_blank:
                beqz t1, %1$s_signed
                lbu t2, 0(t0)
                li t3, 32
                beq t2, t3, %1$s_skip
                li t3, 9
                bne t2, t3, %1$s_sign
            %1$s_skip:
                addi t0, t0, 1
                addi t1, t1, -1
                j %1$s_blank
            %1$s_sign:
                li t3, 45
                bne t2, t3, %1$s_plus
                li t5, 1
                j %1$s_advance
            %1$s_plus:
                li t3, 43
                bne t2, t3, %1$s_signed
            %1$s_advance:
                addi t0, t0, 1
                addi t1, t1, -1
            %1$s_signed:
            """.replace("%1$s", prefix);
    }

    private static final String PARSE = """
            # str_to_int(a0 = string record) -> a0 = integer. Blanks and a sign may come first; reading stops
            # at the first character that is not a digit, and text without digits is 0.
            str_to_int:
                ld t1, 0(a0)
                addi t0, a0, 8
                li a0, 0
            %s
            si_digits:
                beqz t1, si_done
                lbu t2, 0(t0)
                addi t2, t2, -48
                li t3, 10
                bgeu t2, t3, si_done         # not a digit
                mul a0, a0, t3
                add a0, a0, t2
                addi t0, t0, 1
                addi t1, t1, -1
                j si_digits
            si_done:
                beqz t5, si_ret
                neg a0, a0
            si_ret:
                ret

            # str_to_float(a0 = string record) -> a0 = bits of a double. All digits form one integer
            # mantissa that is divided once by 10^k, where k = digits after the point minus the exponent
            # (a negative k multiplies), so the result is the closest double for numbers of up to 15
            # significant digits. An exponent is `e` or `E`, an optional sign and digits (1.5e3, 2E-2).
            str_to_float:
                ld t1, 0(a0)
                addi t0, a0, 8
                li a0, 0
                li t6, 0                     # digits after the point, later reduced by the exponent
                li a1, 0                     # 1 once the point has been seen
            %s
            sf_digits:
                beqz t1, sf_scale
                lbu t2, 0(t0)
                li t3, 46
                bne t2, t3, sf_digit
                bnez a1, sf_scale            # a second point ends the number
                li a1, 1
                j sf_next
            sf_digit:
                li t3, 101
                beq t2, t3, sf_exp
                li t3, 69
                beq t2, t3, sf_exp
                addi t2, t2, -48
                li t3, 10
                bgeu t2, t3, sf_scale
                mul a0, a0, t3
                add a0, a0, t2
                add t6, t6, a1               # counts fraction digits only after the point
            sf_next:
                addi t0, t0, 1
                addi t1, t1, -1
                j sf_digits
            sf_exp:
                addi t0, t0, 1
                addi t1, t1, -1
                li a2, 0                     # exponent magnitude
                li a3, 0                     # 1 when the exponent is negative
                beqz t1, sf_scale
                lbu t2, 0(t0)
                li t3, 45
                bne t2, t3, sf_exp_plus
                li a3, 1
                j sf_exp_skip
            sf_exp_plus:
                li t3, 43
                bne t2, t3, sf_exp_digits
            sf_exp_skip:
                addi t0, t0, 1
                addi t1, t1, -1
            sf_exp_digits:
                beqz t1, sf_exp_done
                lbu t2, 0(t0)
                addi t2, t2, -48
                li t3, 10
                bgeu t2, t3, sf_exp_done
                li t4, 400                   # beyond this the double is 0 or infinity anyway
                bge a2, t4, sf_exp_more
                mul a2, a2, t3
                add a2, a2, t2
            sf_exp_more:
                addi t0, t0, 1
                addi t1, t1, -1
                j sf_exp_digits
            sf_exp_done:
                beqz a3, sf_exp_positive
                add t6, t6, a2
                j sf_scale
            sf_exp_positive:
                sub t6, t6, a2
            sf_scale:
                fcvt.d.l ft0, a0
                beqz a0, sf_apply_sign             # zero stays zero whatever the exponent
                li t0, 1
                fcvt.d.l ft1, t0             # ft1 = 10^k, built by repeated multiplication (exact up to 10^22)
                li t3, 10
                fcvt.d.l ft2, t3
                bltz t6, sf_negative
            sf_pow:
                beqz t6, sf_divide
                fmul.d ft1, ft1, ft2
                addi t6, t6, -1
                j sf_pow
            sf_divide:
                fdiv.d ft0, ft0, ft1
                j sf_apply_sign
            sf_negative:
                beqz t6, sf_multiply
                fmul.d ft1, ft1, ft2
                addi t6, t6, 1
                j sf_negative
            sf_multiply:
                fmul.d ft0, ft0, ft1
            sf_apply_sign:
                beqz t5, sf_ret
                fneg.d ft0, ft0
            sf_ret:
                fmv.x.d a0, ft0
                ret

            """;
}
