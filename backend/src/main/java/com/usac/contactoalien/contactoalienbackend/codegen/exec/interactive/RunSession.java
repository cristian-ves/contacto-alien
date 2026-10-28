package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

import com.usac.contactoalien.contactoalienbackend.codegen.exec.ExecMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.ScratchDirectory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
One running program with a live console: its output goes to an event log, its input comes from a
queue, and a monitor thread enforces the limits. The CPU limit counts processing time only
(waiting for the user costs none); the total duration and the time without activity are bounded too.
When the process ends, the temporary directory is removed and the exit event closes the log.
*/
public final class RunSession {

    private static final long MONITOR_INTERVAL_MILLIS = 200;
    private static final long STREAM_JOIN_MILLIS = 2000;
    // Standard error is logged this much later, so output written just before an error message comes first.
    private static final long STDERR_SETTLE_MILLIS = 10;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long MILLIS_PER_SECOND = 1000L;

    private final String id;
    private final Path directory;
    private final Process process;
    private final RunLimits limits;
    private final int cpuLimitSeconds;
    private final int maxOutputBytes;
    private final Runnable onFinished;
    private final EventLog log = new EventLog();
    private final InputFeeder feeder;
    private final long startNanos = System.nanoTime();
    private final AtomicReference<StopReason> stopReason = new AtomicReference<>();
    private final CountDownLatch finished = new CountDownLatch(1);
    private volatile long lastActivityNanos = startNanos;
    private volatile long detachedSinceNanos = startNanos;
    private volatile long finishedAtNanos;
    private volatile long cpuMillis;
    private volatile boolean exited;
    private int attachedStreams;

    RunSession(String id, Path directory, Process process, RunLimits limits, int cpuLimitSeconds,
               int maxOutputBytes, Runnable onFinished) {
        this.id = id;
        this.directory = directory;
        this.process = process;
        this.limits = limits;
        this.cpuLimitSeconds = cpuLimitSeconds;
        this.maxOutputBytes = maxOutputBytes;
        this.onFinished = onFinished;
        this.feeder = new InputFeeder(process.getOutputStream(), limits.maxPendingInputBytes());
    }

    // Starts the threads that move data and watch the process; preloaded text is queued for stdin first.
    void begin(String preloadedInput) {
        if (preloadedInput != null && !preloadedInput.isEmpty()) {
            feeder.offer(preloadedInput.getBytes(StandardCharsets.UTF_8), false);
        }
        feeder.start();
        Thread stdout = Thread.ofVirtual().start(new StreamPump(process.getInputStream(), RunEvent.Kind.STDOUT, log,
                maxOutputBytes, 0, this::touch));
        Thread stderr = Thread.ofVirtual().start(new StreamPump(process.getErrorStream(), RunEvent.Kind.STDERR, log,
                maxOutputBytes, STDERR_SETTLE_MILLIS, this::touch));
        Thread.ofVirtual().start(() -> monitor(stdout, stderr));
    }

    public String id() {
        return id;
    }

    public RunEvent awaitEvent(int index, long timeoutMs) throws InterruptedException {
        return log.await(index, timeoutMs);
    }

    // True once the log has been complete for longer than the retention period, so the session can be forgotten.
    boolean isExpired(long nowNanos) {
        return finishedAtNanos != 0
                && nowNanos - finishedAtNanos > TimeUnit.SECONDS.toNanos(limits.finishedRetentionSeconds());
    }

    public InputOutcome sendInput(String text) {
        if (exited) {
            return InputOutcome.SESSION_ENDED;
        }
        touch();
        return InputOutcome.from(feeder.offer(text.getBytes(StandardCharsets.UTF_8), true));
    }

    public InputOutcome closeInput() {
        if (exited) {
            return InputOutcome.SESSION_ENDED;
        }
        touch();
        return InputOutcome.from(feeder.endOfInput());
    }

    public void stop() {
        stop(StopReason.USER);
    }

    void stop(StopReason reason) {
        if (stopReason.compareAndSet(null, reason)) {
            process.destroyForcibly();
        }
    }

    // A stream (a console) started following this session.
    public synchronized void streamAttached() {
        attachedStreams++;
        touch();
    }

    public synchronized void streamDetached() {
        attachedStreams--;
        if (attachedStreams == 0) {
            detachedSinceNanos = System.nanoTime();
        }
    }

    boolean awaitFinished(long timeoutMs) throws InterruptedException {
        return finished.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void touch() {
        lastActivityNanos = System.nanoTime();
    }

    // Samples the CPU time and applies the limits every interval until the process ends, then wraps up.
    private void monitor(Thread stdout, Thread stderr) {
        try {
            while (!process.waitFor(MONITOR_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)) {
                enforceLimits();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        exited = true;
        try {
            stdout.join(STREAM_JOIN_MILLIS);
            stderr.join(STREAM_JOIN_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        wrapUp();
    }

    private void enforceLimits() {
        long cpu = ProcessCpuClock.cpuMillis(process.pid());
        if (cpu >= 0) {
            cpuMillis = cpu;
        }
        long now = System.nanoTime();
        if (cpuMillis >= cpuLimitSeconds * MILLIS_PER_SECOND) {
            stop(StopReason.CPU_LIMIT);
        } else if (now - startNanos >= TimeUnit.SECONDS.toNanos(limits.wallSeconds())) {
            stop(StopReason.WALL_LIMIT);
        } else if (now - lastActivityNanos >= TimeUnit.SECONDS.toNanos(limits.idleSeconds())) {
            stop(StopReason.IDLE);
        } else if (isDetachedTooLong(now)) {
            stop(StopReason.DETACHED);
        }
    }

    private synchronized boolean isDetachedTooLong(long now) {
        return attachedStreams == 0
                && now - detachedSinceNanos >= TimeUnit.SECONDS.toNanos(limits.detachGraceSeconds());
    }

    private void wrapUp() {
        feeder.shutdown();
        StopReason reason = stopReason.get();
        int exitCode = process.exitValue();
        String note = null;
        if (reason != null) {
            exitCode = reason.exitCode();
            note = reason.note(limitFor(reason));
        } else if (exitCode > ExecMessages.SIGNAL_EXIT_BASE) {
            note = ExecMessages.KILLED_BY_SIGNAL.formatted(exitCode - ExecMessages.SIGNAL_EXIT_BASE);
        }
        ScratchDirectory.delete(directory);
        log.appendExit(exitCode, (System.nanoTime() - startNanos) / NANOS_PER_MILLI, cpuMillis, note);
        finishedAtNanos = System.nanoTime();
        finished.countDown();
        onFinished.run();
    }

    private int limitFor(StopReason reason) {
        return switch (reason) {
            case CPU_LIMIT -> cpuLimitSeconds;
            case WALL_LIMIT -> limits.wallSeconds();
            case IDLE -> limits.idleSeconds();
            default -> 0;
        };
    }
}
