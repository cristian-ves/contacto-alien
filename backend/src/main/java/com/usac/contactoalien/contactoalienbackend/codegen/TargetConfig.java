package com.usac.contactoalien.contactoalienbackend.codegen;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
Toolchain commands (read from application.properties) plus the platform constants of the
RV64 Linux target. Word size and system call numbers live only here and in the runtime
library, so changing the target (for example to Spike with pk) touches nothing else.
*/
@ConfigurationProperties(prefix = "compiler.target")
public record TargetConfig(String assembler, List<String> assemblerFlags,
                           String linker, List<String> linkerFlags,
                           String simulator, List<String> simulatorFlags,
                           int timeoutSeconds, int maxOutputBytes) {

    // Every value (int, string pointer, object pointer) occupies one 64-bit word.
    public static final int WORD_SIZE = 8;
    public static final int STACK_ALIGNMENT = 16;

    public static final int SYS_READ = 63;
    public static final int SYS_WRITE = 64;
    public static final int SYS_EXIT = 93;
    public static final int FD_STDIN = 0;
    public static final int FD_STDOUT = 1;
    public static final int FD_STDERR = 2;
    // Unit of the CPU times in /proc/<pid>/stat (USER_HZ), used to measure the CPU time of a running program.
    public static final int CLOCK_TICKS_PER_SECOND = 100;

    public static final long HEAP_ARENA_BYTES = 128L * 1024 * 1024;
    // The simulator is started with a stack of STACK_SIZE_BYTES (compiler.target.simulator-flags); the program checks
    // against STACK_BUDGET_BYTES so it stops with a message before the guest stack really runs out.
    public static final long STACK_SIZE_BYTES = 64L * 1024 * 1024;
    public static final long STACK_BUDGET_BYTES = 60L * 1024 * 1024;
    public static final int OUT_OF_MEMORY_EXIT_CODE = 12;
    public static final int RUNTIME_ERROR_EXIT_CODE = 1;

    public TargetConfig {
        assemblerFlags = assemblerFlags == null ? List.of() : assemblerFlags;
        linkerFlags = linkerFlags == null ? List.of() : linkerFlags;
        simulatorFlags = simulatorFlags == null ? List.of() : simulatorFlags;
    }
}
