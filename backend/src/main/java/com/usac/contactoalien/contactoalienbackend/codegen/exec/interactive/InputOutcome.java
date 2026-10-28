package com.usac.contactoalien.contactoalienbackend.codegen.exec.interactive;

// Result of handing text (or end of input) to a session.
public enum InputOutcome {
    ACCEPTED,
    // The program already ended.
    SESSION_ENDED,
    // The standard input of the program was already closed by the user.
    INPUT_CLOSED,
    // Too much unread input is pending.
    TOO_MUCH_PENDING;

    static InputOutcome from(InputFeeder.Offer offer) {
        return switch (offer) {
            case ACCEPTED -> ACCEPTED;
            case CLOSED -> INPUT_CLOSED;
            case FULL -> TOO_MUCH_PENDING;
        };
    }
}
