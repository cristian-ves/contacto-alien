package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

/**
Runtime routines to show decimal numbers (IEEE doubles kept as 64-bit words). print_float and
float_to_str are the only place that decides how many decimals are shown: exactly two, rounded
half up. Parsing text back into numbers lives in RuntimeParsing. Conventions are those of RuntimeLibrary.
*/
final class RuntimeNumbers {

    private RuntimeNumbers() {
    }

    static String text() {
        return FORMAT;
    }

    private static final String FORMAT = """
            # float_to_str(a0 = bits of a double) -> a0 = new string record with exactly two decimals.
            # The value is scaled by 100 and rounded half up to whole hundredths, then the digits are
            # written right to left into a stack buffer (sp+32..sp+96). "-0.00" is printed as "0.00";
            # infinities and NaN (a division by zero, for instance) give "inf", "-inf" and "nan".
            float_to_str:
                addi sp, sp, -112
                sd ra, 104(sp)
                fmv.d.x ft0, a0
                fclass.d t0, ft0
                andi t1, t0, 1               # bit 0: negative infinity
                bnez t1, ftos_ninf
                andi t1, t0, 128             # bit 7: positive infinity
                bnez t1, ftos_inf
                andi t1, t0, 768             # bits 8 and 9: signaling and quiet NaN
                bnez t1, ftos_nan
                fmv.d.x ft1, zero
                flt.d t5, ft0, ft1           # t5 = 1 when negative
                fabs.d ft0, ft0
                li t0, 100
                fcvt.d.l ft2, t0
                fmul.d ft0, ft0, ft2
                li t0, 0x3FE0000000000000    # 0.5
                fmv.d.x ft3, t0
                fadd.d ft0, ft0, ft3
                fcvt.l.d t1, ft0, rdn        # t1 = hundredths
                bnez t1, ftos_digits
                li t5, 0
            ftos_digits:
                addi t0, sp, 96              # write position, moves left
                li t2, 100
                remu t3, t1, t2              # fraction in hundredths
                divu t1, t1, t2              # integer part
                li t2, 10
                remu t4, t3, t2
                divu t3, t3, t2
                addi t4, t4, 48
                addi t0, t0, -1
                sb t4, 0(t0)                 # hundredths digit
                addi t3, t3, 48
                addi t0, t0, -1
                sb t3, 0(t0)                 # tenths digit
                addi t0, t0, -1
                li t4, 46
                sb t4, 0(t0)                 # '.'
            ftos_int:
                remu t4, t1, t2
                divu t1, t1, t2
                addi t4, t4, 48
                addi t0, t0, -1
                sb t4, 0(t0)
                bnez t1, ftos_int
                beqz t5, ftos_copy
                addi t0, t0, -1
                li t4, 45                    # '-'
                sb t4, 0(t0)
            ftos_copy:
                sd t0, 0(sp)                 # 0(sp)  = first character
                addi t1, sp, 96
                sub t1, t1, t0
                sd t1, 8(sp)                 # 8(sp)  = character count
                addi a0, t1, 8
                call alloc
                sd a0, 16(sp)                # 16(sp) = new record
                ld t0, 8(sp)
                sd t0, 0(a0)
                addi a0, a0, 8
                ld a1, 0(sp)
                ld a2, 8(sp)
                call memcpy
                ld a0, 16(sp)
                j ftos_done
            ftos_ninf:
                la a0, str_ninf
                j ftos_done
            ftos_inf:
                la a0, str_inf
                j ftos_done
            ftos_nan:
                la a0, str_nan
            ftos_done:
                ld ra, 104(sp)
                addi sp, sp, 112
                ret

            # float_rem(a0 = bits of x, a1 = bits of y) -> a0 = bits of x % y with the semantics of Java: the result
            # takes the sign of x and is exact. It is NaN when y is 0, x is infinite or either one is NaN, and it
            # is x when y is infinite. |x| is reduced by the largest y * 2^k that still fits, until |x| < |y|.
            float_rem:
                fmv.d.x ft0, a0
                fmv.d.x ft1, a1
                feq.d t0, ft0, ft0
                beqz t0, frem_nan
                feq.d t0, ft1, ft1
                beqz t0, frem_nan
                fabs.d ft2, ft1              # ft2 = |y|
                fmv.d.x ft3, zero
                feq.d t0, ft2, ft3
                bnez t0, frem_nan            # y is zero
                fclass.d t0, ft0
                andi t0, t0, 129             # bits 0 and 7: infinities
                bnez t0, frem_nan
                fclass.d t0, ft1
                andi t0, t0, 129
                bnez t0, frem_same           # y infinite: x is the remainder
                fabs.d ft4, ft0              # ft4 = |x|
            frem_outer:
                flt.d t0, ft4, ft2
                bnez t0, frem_sign           # |x| < |y|: done
                fmv.d ft5, ft2               # ft5 = |y| * 2^k
            frem_grow:
                fadd.d ft6, ft5, ft5
                fle.d t0, ft6, ft4
                beqz t0, frem_take
                fmv.d ft5, ft6
                j frem_grow
            frem_take:
                fsub.d ft4, ft4, ft5         # exact: both are within a factor of 2
                j frem_outer
            frem_sign:
                fsgnj.d ft4, ft4, ft0        # sign of the dividend
                fmv.x.d a0, ft4
            frem_same:
                ret
            frem_nan:
                li a0, 0x7FF8000000000000
                ret

            # print_float(a0 = bits of a double)
            print_float:
                addi sp, sp, -16
                sd ra, 8(sp)
                call float_to_str
                call print_str
                ld ra, 8(sp)
                addi sp, sp, 16
                ret

            """;
}
