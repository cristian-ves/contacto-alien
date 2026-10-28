package com.usac.contactoalien.contactoalienbackend.errors;

// Distinguishes at which stage of the pipeline an error was produced.
public enum ErrorPhase {
    LEXICAL,
    SYNTACTIC,
    SEMANTIC,
    // The program is valid but uses something the code generator does not translate yet.
    GENERATION,
    // Unexpected failure inside the compiler itself, not attributable to the source code.
    INTERNAL,
    // A problem around running the program rather than in its source (session limit, unknown session, rejected input).
    RUNTIME
}
