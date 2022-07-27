package com.workflow.workflow.utils;

/**
 * Type for representing spring error messages.
 */
public record ErrorType(long timestamp, long status, String error, String message, String path) {
}
