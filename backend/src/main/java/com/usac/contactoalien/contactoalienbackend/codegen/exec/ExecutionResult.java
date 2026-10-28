package com.usac.contactoalien.contactoalienbackend.codegen.exec;

// Outcome of assembling, linking and running a generated program.
public record ExecutionResult(String stdout, String stderr, int exitCode) {
}
