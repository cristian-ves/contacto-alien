package com.usac.contactoalien.contactoalienbackend.api.run;

// User-visible messages of the interactive run endpoints, kept in one place.
final class RunMessages {

    static final String TOO_MANY_SESSIONS = "Too many programs are running at the moment. Stop one or try again shortly.";
    static final String UNKNOWN_SESSION = "The run session does not exist or has expired.";
    static final String SESSION_ENDED = "The program has already finished, so it cannot receive input.";
    static final String INPUT_CLOSED = "The input of this program was already closed.";
    static final String TOO_MUCH_INPUT = "The program has not read the input already sent; wait before sending more.";

    private RunMessages() {
    }
}
