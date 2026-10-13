package com.usac.contactoalien.contactoalienbackend.errors; import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer; import java.util.ArrayList;
import java.util.Collections;
import java.util.List; // Shared error listener for all three languages' lexers and parsers: accumulates
// errors instead of printing to stderr, tagged with the phase it was attached to.
public class CollectingErrorListener extends BaseErrorListener {
    private final ErrorPhase phase;
    private final List<CompilerError> errors = new ArrayList<>();

    public CollectingErrorListener(ErrorPhase phase) {
        this.phase = phase;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        errors.add(new CompilerError(line, charPositionInLine, msg, phase));
    }

    public List<CompilerError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}