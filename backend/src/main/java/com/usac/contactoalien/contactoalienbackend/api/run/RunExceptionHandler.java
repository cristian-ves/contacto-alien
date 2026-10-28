package com.usac.contactoalien.contactoalienbackend.api.run;

import com.usac.contactoalien.contactoalienbackend.api.validate.ValidateResponse;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
Turns a RunException into its HTTP status with the standard error body. It runs before the
catch-all handler, which answers everything with 200, and sets the content type explicitly because
the stream endpoint asks for text/event-stream.
*/
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RunExceptionHandler {

    @ExceptionHandler(RunException.class)
    public ResponseEntity<ValidateResponse> handle(RunException e) {
        CompilerError error = new CompilerError(1, 1, e.getMessage(), ErrorPhase.RUNTIME);
        return ResponseEntity.status(e.status()).contentType(MediaType.APPLICATION_JSON)
                .body(new ValidateResponse(false, List.of(error)));
    }
}
