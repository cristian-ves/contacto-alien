package com.usac.contactoalien.contactoalienbackend.codegen.exec;

import com.usac.contactoalien.contactoalienbackend.codegen.TargetConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
Assembles and links a generated program inside a directory and builds the command that runs it.
Shared by the batch runner and the interactive sessions so both go through the same toolchain.
*/
@Component
public class ToolchainBuilder {

    private static final String SOURCE_FILE = "program.s";
    private static final String OBJECT_FILE = "program.o";
    private static final String EXECUTABLE_FILE = "program";

    private record Tool(String role, String installPackage, String property) {
    }

    private static final Tool ASSEMBLER = new Tool(ExecMessages.ROLE_ASSEMBLER, ExecMessages.PACKAGE_BINUTILS,
            ExecMessages.PROPERTY_ASSEMBLER);
    private static final Tool LINKER = new Tool(ExecMessages.ROLE_LINKER, ExecMessages.PACKAGE_BINUTILS,
            ExecMessages.PROPERTY_LINKER);
    private static final Tool SIMULATOR = new Tool(ExecMessages.ROLE_SIMULATOR, ExecMessages.PACKAGE_QEMU,
            ExecMessages.PROPERTY_SIMULATOR);

    private final TargetConfig target;

    public ToolchainBuilder(TargetConfig target) {
        this.target = target;
    }

    // Outcome of a build: either the program is ready to run or failure describes what went wrong.
    public record Build(boolean succeeded, ExecutionResult failure) {
        static Build ok() {
            return new Build(true, null);
        }

        static Build failed(ExecutionResult failure) {
            return new Build(false, failure);
        }
    }

    public Build build(Path directory, String assembly) throws IOException {
        Files.writeString(directory.resolve(SOURCE_FILE), assembly, StandardCharsets.UTF_8);
        ExecutionResult assembled = tool(directory, ExecMessages.ASSEMBLER_FAILED, ASSEMBLER, command(target.assembler(),
                target.assemblerFlags(), "-o", OBJECT_FILE, SOURCE_FILE));
        if (assembled != null) {
            return Build.failed(assembled);
        }
        ExecutionResult linked = tool(directory, ExecMessages.LINKER_FAILED, LINKER, command(target.linker(),
                target.linkerFlags(), "-o", EXECUTABLE_FILE, OBJECT_FILE));
        return linked != null ? Build.failed(linked) : Build.ok();
    }

    // Starts the built program under the simulator, with the directory as its working directory.
    public Process start(Path directory) throws IOException {
        List<String> command = command(target.simulator(), target.simulatorFlags(), "./" + EXECUTABLE_FILE);
        return launch(new ProcessBuilder(command).directory(directory.toFile()), SIMULATOR);
    }

    // Runs an assembler or linker step; returns null on success or a result describing the failure.
    private ExecutionResult tool(Path directory, String failureTitle, Tool tool, List<String> command) throws IOException {
        Process process = launch(new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true), tool);
        process.getOutputStream().close();
        OutputCollector output = new OutputCollector(process.getInputStream(), target.maxOutputBytes());
        Thread reader = Thread.ofVirtual().start(output);
        try {
            if (!process.waitFor(target.timeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            reader.join();
            if (process.exitValue() == 0) {
                return null;
            }
            return new ExecutionResult("", failureTitle + output.text(), ExecMessages.TOOLCHAIN_FAILURE_EXIT_CODE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ExecutionResult("", failureTitle, ExecMessages.TOOLCHAIN_FAILURE_EXIT_CODE);
        }
    }

    // A missing command is the usual failure on a fresh machine, so it gets a message that says what to install.
    private Process launch(ProcessBuilder builder, Tool tool) throws IOException {
        try {
            return builder.start();
        } catch (IOException e) {
            String command = builder.command().get(0);
            String message = isInstalled(command)
                    ? ExecMessages.TOOL_UNAVAILABLE.formatted(tool.role(), command, e.getMessage())
                    : ExecMessages.TOOL_MISSING.formatted(tool.role(), command, tool.installPackage(), tool.property());
            throw new ToolUnavailableException(message, e);
        }
    }

    private boolean isInstalled(String command) {
        if (command.contains(File.separator)) {
            return Files.isExecutable(Path.of(command));
        }
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String directory : path.split(File.pathSeparator)) {
            if (!directory.isEmpty() && Files.isExecutable(Path.of(directory, command))) {
                return true;
            }
        }
        return false;
    }

    private List<String> command(String executable, List<String> flags, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(flags);
        command.addAll(List.of(arguments));
        return command;
    }
}
