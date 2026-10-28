package com.usac.contactoalien.contactoalienbackend.api.compile;

import com.usac.contactoalien.contactoalienbackend.codegen.exec.ExecutionResult;
import com.usac.contactoalien.contactoalienbackend.codegen.exec.ProgramRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
Full pipeline behind /api/compile: compile the program (ProgramCompiler stops at the first
failing stage) and run the result to completion with the input given up front.
*/
@Service
public class CompileService {

    private final ProgramCompiler compiler;
    private final ProgramRunner runner;

    public CompileService(ProgramCompiler compiler, ProgramRunner runner) {
        this.compiler = compiler;
        this.runner = runner;
    }

    public CompileResponse compile(CompileRequest request) {
        ProgramCompiler.Compiled compiled = compiler.compile(request);
        if (!compiled.valid()) {
            return CompileResponse.failed(compiled.errors());
        }
        ExecutionResult result = runner.run(compiled.assembly(), request.input());
        return new CompileResponse(true, List.of(), compiled.quadruples(), compiled.assembly(),
                result.stdout(), result.stderr(), result.exitCode());
    }
}
