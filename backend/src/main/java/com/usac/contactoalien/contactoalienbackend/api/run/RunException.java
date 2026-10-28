package com.usac.contactoalien.contactoalienbackend.api.run;

import org.springframework.http.HttpStatus;

// A request about a run session that cannot be honoured; carries the HTTP status and a readable message.
class RunException extends RuntimeException {

    private final HttpStatus status;

    RunException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus status() {
        return status;
    }
}
