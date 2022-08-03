package com.workflow.workflow.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception throwed when exterananl API returns error.
 */
public class ExternalApiException extends ResponseStatusException {

    /**
     * Creates an exception that returns external api error.
     * 
     * @param externalApi api that caused the error
     * @param message     the message of the error
     */
    public ExternalApiException(String externalApi, String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, String.format("Error with external api '%s': %s", externalApi, message));
    }
}
