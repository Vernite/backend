package com.workflow.workflow.utils;

public record ErrorType(long timestamp, long status, String error, String message, String path) {
}
