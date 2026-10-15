package com.usac.contactoalien.contactoalienbackend.grammar;

import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import java.util.List;

/**
Encapsulates the result of a parse attempt, carrying the generated parse tree (if successful)
and all lexical and syntactic errors captured during the process.

@param <T>  The specific type of the ANTLR parse tree root node.
@param tree The root node of the parsed tree, or null if parsing could not produce one.
@param errors The list of lexical and syntactic errors accumulated by error listeners.
*/
public record ParseOutcome<T>(T tree, List<CompilerError> errors) {

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
