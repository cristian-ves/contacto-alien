package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

/**
Runtime routines that build and print numbers and strings: decimal conversion, string
concatenation and the boolean words. They follow the conventions described in RuntimeLibrary.
*/
final class RuntimeStrings {

    private RuntimeStrings() {
    }

    static String text() {
        return TEXT;
    }

    private static final String TEXT = """
            # int_to_digits(a0 = value, a1 = end of a buffer) -> a0 = first digit, a1 = digit count.
            # Writes the decimal digits from right to left, so no reversal is needed. The magnitude is
            # handled as an unsigned number, which also covers the most negative integer.
            int_to_digits:
                mv t0, a1                    # t0 = write position (moves left)
                mv t3, a0                    # t3 = original value, keeps the sign
                mv t1, a0
                bgez t1, itoa_digits
                neg t1, t1                   # t1 = magnitude
            itoa_digits:
                li t2, 10
            itoa_loop:
                remu t4, t1, t2              # last digit
                divu t1, t1, t2
                addi t4, t4, 48              # '0' + digit
                addi t0, t0, -1
                sb t4, 0(t0)
                bnez t1, itoa_loop           # runs once for 0, so 0 prints as "0"
                bgez t3, itoa_done
                addi t0, t0, -1
                li t4, 45                    # '-'
                sb t4, 0(t0)
            itoa_done:
                sub a1, a1, t0               # count = end - start
                mv a0, t0
                ret

            # print_int(a0 = value): converts in a 32-byte stack buffer and writes the digits.
            print_int:
                addi sp, sp, -48
                sd ra, 40(sp)
                addi a1, sp, 32              # end of the buffer sp..sp+32
                call int_to_digits           # a0/a1 are now exactly write_bytes' arguments
                call write_bytes
                ld ra, 40(sp)
                addi sp, sp, 48
                ret

            # int_to_str(a0 = value) -> a0 = new string record on the heap.
            int_to_str:
                addi sp, sp, -80
                sd ra, 72(sp)
                addi a1, sp, 64              # digit buffer is sp+32..sp+64
                call int_to_digits
                sd a0, 0(sp)                 # 0(sp)  = first digit
                sd a1, 8(sp)                 # 8(sp)  = digit count
                addi a0, a1, 8               # record size = 8 + count
                call alloc
                sd a0, 16(sp)                # 16(sp) = new record
                ld t0, 8(sp)
                sd t0, 0(a0)                 # length field
                addi a0, a0, 8
                ld a1, 0(sp)
                ld a2, 8(sp)
                call memcpy
                ld a0, 16(sp)
                ld ra, 72(sp)
                addi sp, sp, 80
                ret

            # char_to_str(a0 = character code) -> a0 = string record of length 1.
            char_to_str:
                addi sp, sp, -32
                sd ra, 24(sp)
                sd a0, 0(sp)
                li a0, 9
                call alloc
                li t0, 1
                sd t0, 0(a0)
                ld t1, 0(sp)
                sb t1, 8(a0)
                ld ra, 24(sp)
                addi sp, sp, 32
                ret

            # bool_to_str(a0 = 0 or 1) -> a0 = pointer to the constant record "verum" or "falsus".
            bool_to_str:
                beqz a0, bool_false
                la a0, str_verum
                ret
            bool_false:
                la a0, str_falsus
                ret

            # print_bool(a0 = 0 or 1)
            print_bool:
                addi sp, sp, -16
                sd ra, 8(sp)
                call bool_to_str
                call print_str
                ld ra, 8(sp)
                addi sp, sp, 16
                ret

            # str_eq(a0 = left record, a1 = right record) -> a0 = 1 when both hold the same bytes, else 0.
            # A null reference equals only another null reference.
            str_eq:
                beqz a0, str_eq_null
                beqz a1, str_eq_no
                ld t0, 0(a0)
                ld t1, 0(a1)
                bne t0, t1, str_eq_no
                addi a0, a0, 8
                addi a1, a1, 8
            str_eq_loop:
                beqz t0, str_eq_yes
                lbu t2, 0(a0)
                lbu t3, 0(a1)
                bne t2, t3, str_eq_no
                addi a0, a0, 1
                addi a1, a1, 1
                addi t0, t0, -1
                j str_eq_loop
            str_eq_null:
                seqz a0, a1
                ret
            str_eq_yes:
                li a0, 1
                ret
            str_eq_no:
                li a0, 0
                ret

            # str_concat(a0 = left record, a1 = right record) -> a0 = new record with both texts.
            # A null reference counts as the text "null", as in Java.
            str_concat:
                bnez a0, str_concat_left
                la a0, str_null
            str_concat_left:
                bnez a1, str_concat_right
                la a1, str_null
            str_concat_right:
                addi sp, sp, -48
                sd ra, 40(sp)
                sd a0, 0(sp)                 # 0(sp)  = left
                sd a1, 8(sp)                 # 8(sp)  = right
                ld t0, 0(a0)
                ld t1, 0(a1)
                add t2, t0, t1
                sd t2, 16(sp)                # 16(sp) = total length
                addi a0, t2, 8
                call alloc
                sd a0, 24(sp)                # 24(sp) = result record
                ld t2, 16(sp)
                sd t2, 0(a0)                 # length field
                addi a0, a0, 8               # destination: start of the result bytes
                ld t3, 0(sp)
                addi a1, t3, 8               # source: left bytes
                ld a2, 0(t3)
                call memcpy
                ld a0, 24(sp)
                addi a0, a0, 8
                ld t3, 0(sp)
                ld t4, 0(t3)
                add a0, a0, t4               # destination: result bytes + left length
                ld t3, 8(sp)
                addi a1, t3, 8               # source: right bytes
                ld a2, 0(t3)
                call memcpy
                ld a0, 24(sp)
                ld ra, 40(sp)
                addi sp, sp, 48
                ret
            """;
}
