package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

import java.util.ArrayList;
import java.util.List;

/**
Append-only log of everything a session produced. Readers ask for the entry at an index and wait
for it, so a stream that attaches late (or reconnects) replays from where it left off and then
follows the program live.
*/
final class EventLog {

    private final List<RunEvent> events = new ArrayList<>();

    synchronized void appendText(RunEvent.Kind kind, String text) {
        events.add(RunEvent.text(events.size(), kind, text));
        notifyAll();
    }

    synchronized void appendExit(int exitCode, long durationMs, long cpuMs, String note) {
        events.add(RunEvent.exit(events.size(), exitCode, durationMs, cpuMs, note));
        notifyAll();
    }

    // Returns the entry at index, waiting up to timeoutMs for it to exist; null when the wait expires.
    synchronized RunEvent await(int index, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        while (index >= events.size()) {
            long remainingMs = (deadline - System.nanoTime()) / 1_000_000L;
            if (remainingMs <= 0) {
                return null;
            }
            wait(remainingMs);
        }
        return events.get(index);
    }
}
