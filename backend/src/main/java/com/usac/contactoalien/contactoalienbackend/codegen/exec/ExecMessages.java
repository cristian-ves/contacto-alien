package com.usac.contactoalien.contactoalienbackend.codegen.exec;

// Messages of the execution stage, kept in one place.
public final class ExecMessages {

    public static final String OUTPUT_TRUNCATED = "\n[output truncated: the output limit was reached]";
    public static final String ASSEMBLER_FAILED = "Assembler failed:\n";
    public static final String LINKER_FAILED = "Linker failed:\n";
    // Arguments: role of the tool, command, apt package, property that configures the command.
    public static final String TOOL_MISSING = "The %s '%s' is not installed or is not on the PATH. "
            + "Install it (Debian/Ubuntu: sudo apt install %s) or set %s in application.properties.";
    public static final String TOOL_UNAVAILABLE = "Could not run the %s '%s': %s";
    public static final String IO_FAILURE = "Could not prepare or run the program: %s";
    public static final String ROLE_ASSEMBLER = "assembler";
    public static final String ROLE_LINKER = "linker";
    public static final String ROLE_SIMULATOR = "simulator";
    public static final String PACKAGE_BINUTILS = "binutils-riscv64-linux-gnu";
    public static final String PACKAGE_QEMU = "qemu-user";
    public static final String PROPERTY_ASSEMBLER = "compiler.target.assembler";
    public static final String PROPERTY_LINKER = "compiler.target.linker";
    public static final String PROPERTY_SIMULATOR = "compiler.target.simulator";
    public static final String TIMED_OUT = "Execution stopped: time limit exceeded (%d s)";

    public static final String STOPPED_BY_USER = "Execution stopped by the user";
    public static final String CPU_LIMIT_EXCEEDED = "Execution stopped: CPU time limit exceeded (%d s of processing)";
    public static final String WALL_LIMIT_EXCEEDED = "Execution stopped: the session reached its maximum duration (%d s)";
    public static final String IDLE_LIMIT_EXCEEDED = "Execution stopped: no activity for %d s while waiting for input";
    public static final String CLIENT_DISCONNECTED = "Execution stopped: the client disconnected";
    public static final String SERVER_SHUTDOWN = "Execution stopped: the server is shutting down";
    public static final String OUTPUT_LIMIT_REACHED = "[output limit reached: the rest of this stream is discarded]";

    public static final String KILLED_BY_SIGNAL = "Execution stopped: the program was terminated by signal %d (invalid memory access)";

    // The shell convention for a process ended by a signal: 128 plus the signal number.
    public static final int SIGNAL_EXIT_BASE = 128;
    // Exit status used when the program is killed for exceeding the time limit (same as timeout(1)).
    public static final int TIMEOUT_EXIT_CODE = 124;
    // Exit status reported when the session was ended from outside (user stop, disconnection, shutdown): 128 + SIGTERM.
    public static final int STOPPED_EXIT_CODE = 143;
    // Exit status reported when the toolchain itself failed, so no program ran.
    public static final int TOOLCHAIN_FAILURE_EXIT_CODE = -1;

    private ExecMessages() {
    }
}
