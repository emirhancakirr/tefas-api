package com.tefasfundapi.tefasFundAPI.exception;

/**
 * Base exception for client layer errors (Playwright, network, etc.).
 */
public class TefasClientException extends TefasException {
    public TefasClientException(String message) {
        super("CLIENT_ERROR", message);
    }

    public TefasClientException(String message, Throwable cause) {
        super("CLIENT_ERROR", message, cause);
    }
}
