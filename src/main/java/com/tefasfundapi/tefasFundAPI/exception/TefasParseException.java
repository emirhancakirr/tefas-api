package com.tefasfundapi.tefasFundAPI.exception;

/**
 * Exception thrown when parsing TEFAS response fails.
 */
public class TefasParseException extends TefasException {
    public TefasParseException(String message) {
        super("PARSE_ERROR", message);
    }

    public TefasParseException(String message, Throwable cause) {
        super("PARSE_ERROR", message, cause);
    }
}
