package com.tefasfundapi.tefasFundAPI.exception;

/**
 * Exception thrown when date range validation fails.
 */
public class InvalidDateRangeException extends TefasException {
    public InvalidDateRangeException(String message) {
        super("BAD_REQUEST", message);
    }
}
