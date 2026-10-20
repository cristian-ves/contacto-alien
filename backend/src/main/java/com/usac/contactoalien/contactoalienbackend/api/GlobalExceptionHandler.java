package com.usac.contactoalien.contactoalienbackend.api;

import com.usac.contactoalien.contactoalienbackend.api.validate.ValidateResponse;
import com.usac.contactoalien.contactoalienbackend.errors.CompilerError;
import com.usac.contactoalien.contactoalienbackend.errors.ErrorPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.Method;
import java.util.List;

/**
Converts any uncaught exception and request deserialization error into the standard
validation response carrying a CompilerError, ensuring no error leaks as HTTP 500.
*/
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MSG_MALFORMED_REQUEST = "Malformed or unreadable request body";
    private static final String MSG_INTERNAL_ERROR = "Internal compiler error while analyzing the source";

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public ValidateResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("Failed to read HTTP request body", ex);
        CompilerError error = new CompilerError(
                1,
                0,
                MSG_MALFORMED_REQUEST,
                ErrorPhase.SYNTACTIC
        );
        return new ValidateResponse(false, List.of(error));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ValidateResponse handleException(Exception ex) {
        log.error("Uncaught exception during compilation/validation", ex);

        int line = extractIntProperty(ex, "getLine", 1);
        int column = extractIntProperty(ex, "getColumn", 0);

        CompilerError error = new CompilerError(line, column, MSG_INTERNAL_ERROR, ErrorPhase.SEMANTIC);
        return new ValidateResponse(false, List.of(error));
    }

    private int extractIntProperty(Object target, String methodName, int defaultValue) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Number number) {
                return number.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Target object does not have the specified getter or reflection is restricted
        }
        return defaultValue;
    }
}
