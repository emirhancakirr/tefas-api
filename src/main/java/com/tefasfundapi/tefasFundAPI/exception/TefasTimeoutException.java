package com.tefasfundapi.tefasFundAPI.exception;

/**
 * Exception thrown when an operation times out.
 */
public class TefasTimeoutException extends TefasClientException {
    public TefasTimeoutException(String operation, long timeoutMs) {
        super(String.format("Operation '%s' timed out after %d ms", operation, timeoutMs));
    }

    public TefasTimeoutException(String operation, long timeoutMs, Throwable cause) {
        super(String.format("Operation '%s' timed out after %d ms", operation, timeoutMs), cause);
    }
}
