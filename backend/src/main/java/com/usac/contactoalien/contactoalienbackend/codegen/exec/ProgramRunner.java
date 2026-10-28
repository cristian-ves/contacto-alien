package com.usac.contactoalien.contactoalienbackend.codegen.exec;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
Builds (through ToolchainBuilder) and runs a generated program inside a throw-away directory. The program runs
with a time limit (the process is killed when it expires), its output is capped, and the
directory is removed at the end whatever happens.
*/
@Component
public class ProgramRunner {

    private final TargetConfig target;
    private final ToolchainBuilder toolchain;

    public ProgramRunner(TargetConfig target, ToolchainBuilder toolchain) {
        this.target = target;
        this.toolchain = toolchain;
    }

    public ExecutionResult run(String assembly, String input) {
        Path directory = null;
        try {
            directory = ScratchDirectory.create();
            ToolchainBuilder.Build build = toolchain.build(directory, assembly);
            if (!build.succeeded()) {
                return build.failure();
            }
            return execute(directory, input);
        } catch (ToolUnavailableException e) {
            return new ExecutionResult("", e.getMessage(), ExecMessages.TOOLCHAIN_FAILURE_EXIT_CODE);
        } catch (IOException e) {
            return new ExecutionResult("", ExecMessages.IO_FAILURE.formatted(e.getMessage()),
                    ExecMessages.TOOLCHAIN_FAILURE_EXIT_CODE);
        } finally {
            ScratchDirectory.delete(directory);
        }
    }

    private ExecutionResult execute(Path directory, String input) throws IOException {
        Process process = toolchain.start(directory);
        OutputCollector stdout = new OutputCollector(process.getInputStream(), target.maxOutputBytes());
        OutputCollector stderr = new OutputCollector(process.getErrorStream(), target.maxOutputBytes());
        Thread stdoutReader = Thread.ofVirtual().start(stdout);
        Thread stderrReader = Thread.ofVirtual().start(stderr);
        Thread feeder = Thread.ofVirtual().start(() -> feed(process, input));
        try {
            boolean finished = process.waitFor(target.timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly().waitFor();
            }
            stdoutReader.join();
            stderrReader.join();
            feeder.interrupt();
            if (!finished) {
                String note = ExecMessages.TIMED_OUT.formatted(target.timeoutSeconds());
                return new ExecutionResult(stdout.text(), stderr.text() + note, ExecMessages.TIMEOUT_EXIT_CODE);
            }
            int exitCode = process.exitValue();
            String note = exitCode > ExecMessages.SIGNAL_EXIT_BASE ? ExecMessages.KILLED_BY_SIGNAL.formatted(exitCode - ExecMessages.SIGNAL_EXIT_BASE) : "";
            return new ExecutionResult(stdout.text(), stderr.text() + note, exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ExecutionResult(stdout.text(), stderr.text(), ExecMessages.TOOLCHAIN_FAILURE_EXIT_CODE);
        }
    }

    // Writes the program's standard input and closes it, so reads see end of file afterwards.
    private void feed(Process process, String input) {
        try (OutputStream stdin = process.getOutputStream()) {
            if (input != null) {
                stdin.write(input.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // The program may exit without reading its input.
        }
    }
}
