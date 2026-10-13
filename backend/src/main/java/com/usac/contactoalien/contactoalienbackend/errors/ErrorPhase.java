package com.usac.contactoalien.contactoalienbackend.errors; // Distinguishes at which stage of the pipeline an error was produced.
public enum ErrorPhase {
    LEXICAL,
    SYNTACTIC,
    SEMANTIC
}