package com.usac.contactoalien.contactoalienbackend.api.run;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.Quad;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;

import java.util.List;

/**
Result of POST /api/run. When valid is false the errors explain why and no session exists;
otherwise sessionId names the running program and the quadruples and assembly are included.
*/
public record RunResponse(boolean valid, List<CompilerError> errors, String sessionId, List<Quad> quadruples,
                          String assembly) {

    static RunResponse failed(List<CompilerError> errors) {
        return new RunResponse(false, errors, null, List.of(), null);
    }

    static RunResponse failed(List<CompilerError> errors, List<Quad> quadruples, String assembly) {
        return new RunResponse(false, errors, null, quadruples, assembly);
    }
}
