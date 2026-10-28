package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

import com.usac.contactoalien.contactoalienbackend.codegen.exec.ExecMessages;

// Why a session's process was ended from outside; decides the exit code and the note shown to the user.
enum StopReason {
    USER(ExecMessages.STOPPED_EXIT_CODE, ExecMessages.STOPPED_BY_USER),
    CPU_LIMIT(ExecMessages.TIMEOUT_EXIT_CODE, ExecMessages.CPU_LIMIT_EXCEEDED),
    WALL_LIMIT(ExecMessages.TIMEOUT_EXIT_CODE, ExecMessages.WALL_LIMIT_EXCEEDED),
    IDLE(ExecMessages.TIMEOUT_EXIT_CODE, ExecMessages.IDLE_LIMIT_EXCEEDED),
    DETACHED(ExecMessages.STOPPED_EXIT_CODE, ExecMessages.CLIENT_DISCONNECTED),
    SHUTDOWN(ExecMessages.STOPPED_EXIT_CODE, ExecMessages.SERVER_SHUTDOWN);

    private final int exitCode;
    private final String note;

    StopReason(int exitCode, String note) {
        this.exitCode = exitCode;
        this.note = note;
    }

    int exitCode() {
        return exitCode;
    }

    // The note may hold one %d for the limit that was exceeded; the other notes ignore the argument.
    String note(int limit) {
        return note.formatted(limit);
    }
}
