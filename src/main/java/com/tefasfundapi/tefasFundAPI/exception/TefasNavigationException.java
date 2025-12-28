package com.tefasfundapi.tefasFundAPI.exception;

/**
 * Exception thrown when navigation to a page fails.
 */
public class TefasNavigationException extends TefasClientException {
    public TefasNavigationException(String url) {
        super("Failed to navigate to " + url);
    }

    public TefasNavigationException(String url, Throwable cause) {
        super("Failed to navigate to " + url, cause);
    }
}
