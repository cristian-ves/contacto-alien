package com.usac.contactoalien.contactoalienbackend.codegen.riscv;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;

/**
The runtime written in RV64 assembler and appended to every program (memory and output here,
text routines in RuntimeStrings, decimals in RuntimeNumbers, number parsing in RuntimeParsing, input in
RuntimeInput). Calling convention: arguments in a0, a1, a2; result in a0; t0-t5 and a0-a7 may be clobbered; ra is saved by the
routines that call others. A string is a record: a 64-bit byte length followed by the bytes.
Heap objects come from a bump allocator over a fixed arena in .bss; nothing is freed.
This is the only place, besides TargetConfig, that knows system call numbers and the arena size.
*/
final class RuntimeLibrary {

    private RuntimeLibrary() {
    }

    static String text() {
        return TEXT.formatted(TargetConfig.SYS_EXIT, TargetConfig.OUT_OF_MEMORY_EXIT_CODE, TargetConfig.FD_STDERR, TargetConfig.FD_STDOUT,
                TargetConfig.SYS_WRITE, TargetConfig.RUNTIME_ERROR_EXIT_CODE) + STACK.formatted(TargetConfig.STACK_BUDGET_BYTES) + RuntimeStrings.text() + RuntimeNumbers.text() + RuntimeParsing.text() + RuntimeInput.text()
                + DATA.formatted(TargetConfig.HEAP_ARENA_BYTES, TargetConfig.HEAP_ARENA_BYTES) + RuntimeInput.data();
    }

    private static final String TEXT = """
            # ---------------------------------------------------------------- runtime
            # exit(a0 = status): Linux system call exit
            exit:
                li a7, %d
                ecall

            # alloc(a0 = bytes) -> a0 = address of a zero-filled block.
            # Bump allocator: the arena starts zeroed (.bss), so objects begin with all fields at 0/null.
            alloc:
                addi a0, a0, 7
                andi a0, a0, -8              # round the size up to a multiple of 8
                la t0, heap_ptr
                ld t1, 0(t0)                 # t1 = first free byte
                add t2, t1, a0               # t2 = new first free byte
                la t3, heap_end
                ld t3, 0(t3)
                bgtu t2, t3, alloc_fail      # arena exhausted
                sd t2, 0(t0)
                mv a0, t1
                ret
            alloc_fail:
                la a0, msg_out_of_memory
                call print_err
                li a0, %d
                j exit

            # memcpy(a0 = destination, a1 = source, a2 = byte count): copies byte by byte.
            memcpy:
                beqz a2, memcpy_done
            memcpy_loop:
                lbu t0, 0(a1)
                sb t0, 0(a0)
                addi a0, a0, 1
                addi a1, a1, 1
                addi a2, a2, -1
                bnez a2, memcpy_loop
            memcpy_done:
                ret

            # print_err(a0 = string record): like print_str but to standard error, where the run-time
            # error messages go so a console can tell them apart from the program's own output.
            print_err:
                ld a1, 0(a0)
                addi a0, a0, 8
                li a3, %d
                j write_to

            # write_bytes(a0 = address, a1 = count): writes to standard output, repeating the system
            # call until everything is written (write may accept fewer bytes than requested).
            # write_to does the same on the descriptor held in a3.
            write_bytes:
                li a3, %d
            write_to:
                mv a2, a1                    # a2 = bytes left
                mv a1, a0                    # a1 = cursor
            write_loop:
                blez a2, write_done
                mv a0, a3
                li a7, %d
                ecall                        # a0 = bytes written, or a negative error code
                blez a0, write_done
                add a1, a1, a0
                sub a2, a2, a0
                j write_loop
            write_done:
                ret

            # print_str(a0 = string record): the length is stored in the record, the bytes follow it.
            # A null reference (an unset Zetariano String) prints as "null".
            print_str:
                bnez a0, print_str_ok
                la a0, str_null
            print_str_ok:
                ld a1, 0(a0)
                addi a0, a0, 8
                j write_bytes

            # print_nl: writes a line feed.
            print_nl:
                la a0, newline
                li a1, 1
                j write_bytes

            # array_new(a0 = element count) -> a0 = zeroed array record: a word with the length, then the
            # elements (one word each).
            array_new:
                addi sp, sp, -32
                sd ra, 24(sp)
                bltz a0, array_fail
                sd a0, 0(sp)
                addi a0, a0, 1
                slli a0, a0, 3
                call alloc
                ld t0, 0(sp)
                sd t0, 0(a0)
                ld ra, 24(sp)
                addi sp, sp, 32
                ret

            # array_addr(a0 = array record, a1 = index) -> a0 = address of that element, stopping the
            # program with a message when the array is null or the index is out of range.
            array_addr:
                beqz a0, array_fail
                ld t0, 0(a0)
                bgeu a1, t0, array_fail      # unsigned compare also rejects negative indexes
                slli a1, a1, 3
                add a0, a0, a1
                addi a0, a0, 8
                ret
            array_fail:
                la a0, msg_array_range
                j runtime_error

            # null_fail: a field or array length was read through a null reference.
            null_fail:
                la a0, msg_null_reference
                j runtime_error

            # div_zero: integer division or remainder by zero (decimal division follows IEEE and gives inf/nan).
            div_zero:
                la a0, msg_division_by_zero
            # runtime_error(a0 = message record): prints it and stops with the runtime error status.
            runtime_error:
                call print_err
                li a0, %d
                j exit

            # print_char(a0 = character code): writes one byte (the code is kept on the stack for write).
            print_char:
                addi sp, sp, -16
                sd ra, 8(sp)
                sb a0, 0(sp)
                mv a0, sp
                li a1, 1
                call write_bytes
                ld ra, 8(sp)
                addi sp, sp, 16
                ret
            """;

    // stack_init records the lowest address the stack may reach; every function prologue compares sp against it.
    private static final String STACK = """
            stack_init:
                la t0, stack_limit
                li t1, %d
                sub t1, sp, t1
                sd t1, 0(t0)
                ret

            stack_overflow:
                la a0, msg_stack_overflow
                j runtime_error

            """;

    private static final String DATA = """
                .section .rodata
                .balign 8
            str_verum:
                .quad 5
                .ascii "verum"
                .balign 8
            str_falsus:
                .quad 6
                .ascii "falsus"
                .balign 8
            str_nan:
                .quad 3
                .ascii "nan"
                .balign 8
            str_inf:
                .quad 3
                .ascii "inf"
                .balign 8
            str_ninf:
                .quad 4
                .ascii "-inf"
                .balign 8
            str_null:
                .quad 4
                .ascii "null"
                .balign 8
            msg_out_of_memory:
                .quad 14
                .ascii "Out of memory\\n"
                .balign 8
            msg_array_range:
                .quad 20
                .ascii "Index out of bounds\\n"
                .balign 8
            msg_null_reference:
                .quad 15
                .ascii "Null reference\\n"
                .balign 8
            msg_stack_overflow:
                .quad 15
                .ascii "Stack overflow\\n"
                .balign 8
            msg_division_by_zero:
                .quad 17
                .ascii "Division by zero\\n"
                .balign 8
            newline:
                .byte 10

                .data
                .balign 8
            heap_ptr:
                .quad heap_arena
            heap_end:
                .quad heap_arena + %d
            stack_limit:
                .quad 0

                .bss
                .balign 16
            heap_arena:
                .space %d
            """;
}
