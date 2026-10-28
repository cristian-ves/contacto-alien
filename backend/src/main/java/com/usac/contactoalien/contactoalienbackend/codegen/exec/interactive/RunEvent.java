package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

/**
One entry of a session's output log. TEXT kinds carry a fragment of text; EXIT is always the last
entry and carries the exit code, the wall time, the CPU time and an optional note.
*/
public record RunEvent(int id, Kind kind, String text, int exitCode, long durationMs, long cpuMs, String note) {

    public enum Kind {
        STDOUT("stdout"), STDERR("stderr"), SYSTEM("system"), EXIT("exit");

        private final String eventName;

        Kind(String eventName) {
            this.eventName = eventName;
        }

        public String eventName() {
            return eventName;
        }
    }

    static RunEvent text(int id, Kind kind, String text) {
        return new RunEvent(id, kind, text, 0, 0, 0, null);
    }

    static RunEvent exit(int id, int exitCode, long durationMs, long cpuMs, String note) {
        return new RunEvent(id, Kind.EXIT, "", exitCode, durationMs, cpuMs, note);
    }

    public boolean isExit() {
        return kind == Kind.EXIT;
    }
}
