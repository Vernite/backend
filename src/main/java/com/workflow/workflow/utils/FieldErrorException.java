package com.workflow.workflow.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creates an exception that returns field error.
 */
public class FieldErrorException extends ResponseStatusException {

    /**
     * Creates an exception that returns field error.
     * 
     * @param fieldName the name of the field that caused the error
     * @param message   the message of the error
     */
    public FieldErrorException(String fieldName, String message) {
        super(HttpStatus.BAD_REQUEST, String.format("Error with field '%s': %s", fieldName, message));
    }
}
