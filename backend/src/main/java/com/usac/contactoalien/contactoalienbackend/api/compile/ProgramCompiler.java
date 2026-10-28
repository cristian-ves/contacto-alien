package com.usac.contactoalien.contactoalienbackend.api.compile;

import com.usac.contactoalien.contactoalienbackend.api.validate.ValidateRequest;
import com.usac.contactoalien.contactoalienbackend.api.validate.ValidateResponse;
import com.usac.contactoalien.contactoalienbackend.api.validate.ValidatorService;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationException;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.GenerationMessages;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.ProgramGenerator;
import com.usac.contactoalien.contactoalienbackend.codegen.gen.pig.PigProgramGenerator;
import com.usac.contactoalien.contactoalienbackend.codegen.quad.Quad;
import com.usac.contactoalien.contactoalienbackend.codegen.riscv.AssemblyGenerator;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;
import org.springframework.stereotype.Service;

import java.util.List;

/**
Source to assembly: validates the program, translates it to quadruples and then to RISC-V.
Shared by the batch endpoint (/api/compile) and the interactive one (/api/run), which differ only in how they run the result.
*/
@Service
public class ProgramCompiler {

    private static final String PIG = "pig";

    // Either errors (and nothing else) or the quadruples and assembly of a valid program.
    public record Compiled(List<CompilerError> errors, List<Quad> quadruples, String assembly) {

        public boolean valid() {
            return errors.isEmpty();
        }

        static Compiled failed(List<CompilerError> errors) {
            return new Compiled(errors, List.of(), null);
        }
    }

    private final ValidatorService validator;

    public ProgramCompiler(ValidatorService validator) {
        this.validator = validator;
    }

    public Compiled compile(CompileRequest request) {
        if (!PIG.equals(request.language())) {
            return Compiled.failed(List.of(
                    new CompilerError(1, 1, GenerationMessages.ONLY_PIG, ErrorPhase.GENERATION)));
        }
        ValidateResponse validation = validator.validate(
                new ValidateRequest(request.code(), request.language(), request.imports()));
        if (!validation.valid()) {
            return Compiled.failed(validation.errors());
        }
        try {
            List<Quad> quads = new ProgramGenerator().generate(request.code(), request.imports());
            String assembly = new AssemblyGenerator().generate(quads, PigProgramGenerator.ENTRY_LABEL);
            return new Compiled(List.of(), quads, assembly);
        } catch (GenerationException e) {
            return Compiled.failed(List.of(
                    new CompilerError(e.getLine(), e.getColumn(), e.getMessage(), ErrorPhase.GENERATION)));
        }
    }
}
