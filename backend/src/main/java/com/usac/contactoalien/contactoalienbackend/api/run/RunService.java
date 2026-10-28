package com.usac.contactoalien.contactoalienbackend.api.run;

import com.usac.contactoalien.contactoalienbackend.api.compile.CompileRequest;
import com.usac.contactoalien.contactoalienbackend.api.compile.ProgramCompiler;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.ExecMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.ToolUnavailableException;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive.InputOutcome;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive.RunSession;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive.SessionRegistry;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
Behind /api/run: compiles like /api/compile but starts the program as a live session instead of
running it to the end, and routes input, stop and stream requests to the right session.
*/
@Service
public class RunService {

    private final ProgramCompiler compiler;
    private final SessionRegistry registry;

    public RunService(ProgramCompiler compiler, SessionRegistry registry) {
        this.compiler = compiler;
        this.registry = registry;
    }

    public RunResponse start(CompileRequest request) {
        ProgramCompiler.Compiled compiled = compiler.compile(request);
        if (!compiled.valid()) {
            return RunResponse.failed(compiled.errors());
        }
        SessionRegistry.Started started;
        try {
            started = registry.start(compiled.assembly(), request.input());
        } catch (ToolUnavailableException e) {
            return toolchainFailure(e.getMessage(), compiled);
        } catch (IOException e) {
            return toolchainFailure(ExecMessages.IO_FAILURE.formatted(e.getMessage()), compiled);
        }
        if (started == null) {
            throw new RunException(HttpStatus.TOO_MANY_REQUESTS, RunMessages.TOO_MANY_SESSIONS);
        }
        if (started.session() == null) {
            return toolchainFailure(started.failure().stderr(), compiled);
        }
        return new RunResponse(true, List.of(), started.session().id(), compiled.quadruples(), compiled.assembly());
    }

    public RunSession require(String id) {
        RunSession session = registry.find(id);
        if (session == null) {
            throw new RunException(HttpStatus.NOT_FOUND, RunMessages.UNKNOWN_SESSION);
        }
        return session;
    }

    public void input(String id, RunInputRequest request) {
        RunSession session = require(id);
        InputOutcome outcome = InputOutcome.ACCEPTED;
        if (request.text() != null && !request.text().isEmpty()) {
            outcome = session.sendInput(request.text());
        }
        if (outcome == InputOutcome.ACCEPTED && Boolean.TRUE.equals(request.eof())) {
            outcome = session.closeInput();
        }
        switch (outcome) {
            case ACCEPTED -> { }
            case SESSION_ENDED -> throw new RunException(HttpStatus.CONFLICT, RunMessages.SESSION_ENDED);
            case INPUT_CLOSED -> throw new RunException(HttpStatus.CONFLICT, RunMessages.INPUT_CLOSED);
            case TOO_MUCH_PENDING -> throw new RunException(HttpStatus.PAYLOAD_TOO_LARGE, RunMessages.TOO_MUCH_INPUT);
        }
    }

    public void stop(String id) {
        require(id).stop();
    }

    // The assembler or linker failed on code that passed validation: a compiler problem, reported as such.
    private RunResponse toolchainFailure(String message, ProgramCompiler.Compiled compiled) {
        return RunResponse.failed(List.of(new CompilerError(1, 1, message, ErrorPhase.INTERNAL)),
                compiled.quadruples(), compiled.assembly());
    }
}
