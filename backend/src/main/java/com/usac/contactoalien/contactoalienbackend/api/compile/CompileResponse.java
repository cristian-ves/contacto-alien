package com.usac.contactoalien.contactoalienbackend.api.compile;

import com.usac.contactoalien.contactoalienbackend.codegen.quad.Quad;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;

import java.util.List;

/**
Result of /api/compile. When valid is false the errors explain why and nothing was generated;
otherwise the quadruples, the assembly and the program's output are filled in.
*/
public record CompileResponse(boolean valid, List<CompilerError> errors, List<Quad> quadruples,
                              String assembly, String stdout, String stderr, Integer exitCode) {

    public static CompileResponse failed(List<CompilerError> errors) {
        return new CompileResponse(false, errors, List.of(), null, null, null, null);
    }
}
