package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.ExecutionResult;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.ScratchDirectory;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.ToolchainBuilder;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
Creates, finds and ends interactive sessions. It caps how many programs run at once, forgets
sessions a while after they finish, and stops every process when the server shuts down so none is left behind.
*/
@Component
public class SessionRegistry {

    private static final long SHUTDOWN_WAIT_MILLIS = 3000;

    // Either a started session or, when the toolchain failed, the failure to report.
    public record Started(RunSession session, ExecutionResult failure) {
    }

    private final ToolchainBuilder toolchain;
    private final TargetConfig target;
    private final RunLimits limits;
    private final Map<String, RunSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger running = new AtomicInteger();

    public SessionRegistry(ToolchainBuilder toolchain, TargetConfig target, RunLimits limits) {
        this.toolchain = toolchain;
        this.target = target;
        this.limits = limits;
    }

    /**
    Builds and starts the program. Returns null when the limit of simultaneous sessions is reached.
    The input is queued for the program before it starts reading.
    */
    public Started start(String assembly, String input) throws IOException {
        forgetExpired();
        if (!reserveSlot()) {
            return null;
        }
        Path directory = null;
        boolean started = false;
        try {
            directory = ScratchDirectory.create();
            ToolchainBuilder.Build build = toolchain.build(directory, assembly);
            if (!build.succeeded()) {
                return new Started(null, build.failure());
            }
            RunSession session = new RunSession(UUID.randomUUID().toString(), directory, toolchain.start(directory),
                    limits, target.timeoutSeconds(), target.maxOutputBytes(), running::decrementAndGet);
            sessions.put(session.id(), session);
            session.begin(input);
            started = true;
            return new Started(session, null);
        } finally {
            if (!started) {
                ScratchDirectory.delete(directory);
                running.decrementAndGet();
            }
        }
    }

    public RunSession find(String id) {
        forgetExpired();
        return sessions.get(id);
    }

    /**
    Runs as soon as shutdown begins, before the web server waits for requests in flight: an open stream is a
    request in flight, so the sessions must end first (their streams close with an exit event) or the
    server would wait for them until its own timeout.
    */
    @EventListener(ContextClosedEvent.class)
    void shutdown() throws InterruptedException {
        for (RunSession session : sessions.values()) {
            session.stop(StopReason.SHUTDOWN);
        }
        for (RunSession session : sessions.values()) {
            session.awaitFinished(SHUTDOWN_WAIT_MILLIS);
        }
    }

    private boolean reserveSlot() {
        while (true) {
            int current = running.get();
            if (current >= limits.maxSessions()) {
                return false;
            }
            if (running.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private void forgetExpired() {
        long now = System.nanoTime();
        sessions.values().removeIf(session -> session.isExpired(now));
    }
}
