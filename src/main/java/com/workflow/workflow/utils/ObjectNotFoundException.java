package com.workflow.workflow.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creates an exception that returns an object not found on the output.
 * it extends the exception with message "Object not found".
 */
public class ObjectNotFoundException extends ResponseStatusException {

    public ObjectNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Object not found");
    }
}
