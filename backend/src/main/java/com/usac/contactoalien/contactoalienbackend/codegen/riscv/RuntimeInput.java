package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;

/**
Runtime routines that read standard input: one line at a time, with helpers that convert it to
an integer, a decimal or a character. Together with RuntimeLibrary this is where the read
system call is used. The line buffer lives in .bss; a longer line is cut at the buffer size.
*/
final class RuntimeInput {

    static final int LINE_BUFFER_BYTES = 4096;

    private RuntimeInput() {
    }

    static String text() {
        return TEXT.formatted(TargetConfig.FD_STDIN, TargetConfig.SYS_READ, LINE_BUFFER_BYTES - 1);
    }

    static String data() {
        return DATA.formatted(LINE_BUFFER_BYTES);
    }

    private static final String TEXT = """
            # read_line() -> a0 = new string record with the next line of standard input, without its line
            # feed (or carriage return). One byte per system call, so nothing past the line is consumed;
            # end of input ends the line, and a closed input yields the empty string.
            read_line:
                addi sp, sp, -48
                sd ra, 40(sp)
                la t0, line_buf
                sd t0, 0(sp)                 # 0(sp) = where the next byte goes
            read_line_loop:
                li a0, %d
                ld a1, 0(sp)
                li a2, 1
                li a7, %d
                ecall
                blez a0, read_line_end       # 0 = end of input, negative = error
                ld t0, 0(sp)
                lbu t1, 0(t0)
                li t2, 10
                beq t1, t2, read_line_end
                la t3, line_buf
                li t4, %d
                add t3, t3, t4
                bgeu t0, t3, read_line_loop  # buffer full: the extra characters are dropped
                addi t0, t0, 1
                sd t0, 0(sp)
                j read_line_loop
            read_line_end:
                ld t0, 0(sp)
                la t1, line_buf
                sub t2, t0, t1               # t2 = line length
                beqz t2, read_line_make
                lbu t4, -1(t0)
                li t5, 13
                bne t4, t5, read_line_make
                addi t2, t2, -1              # drop the carriage return of a CRLF line
            read_line_make:
                sd t2, 8(sp)
                addi a0, t2, 8
                call alloc
                sd a0, 16(sp)
                ld t0, 8(sp)
                sd t0, 0(a0)
                addi a0, a0, 8
                la a1, line_buf
                mv a2, t0
                call memcpy
                ld a0, 16(sp)
                ld ra, 40(sp)
                addi sp, sp, 48
                ret

            # read_int() -> a0 = the next line as an integer.
            read_int:
                addi sp, sp, -16
                sd ra, 8(sp)
                call read_line
                call str_to_int
                ld ra, 8(sp)
                addi sp, sp, 16
                ret

            # read_float() -> a0 = the next line as the bits of a double.
            read_float:
                addi sp, sp, -16
                sd ra, 8(sp)
                call read_line
                call str_to_float
                ld ra, 8(sp)
                addi sp, sp, 16
                ret

            # read_char() -> a0 = first character of the next line, or 0 when the line is empty.
            read_char:
                addi sp, sp, -16
                sd ra, 8(sp)
                call read_line
                ld t0, 0(a0)
                beqz t0, read_char_empty
                lbu a0, 8(a0)
                j read_char_done
            read_char_empty:
                li a0, 0
            read_char_done:
                ld ra, 8(sp)
                addi sp, sp, 16
                ret

            """;

    private static final String DATA = """
                .bss
                .balign 8
            line_buf:
                .space %d
            """;
}
