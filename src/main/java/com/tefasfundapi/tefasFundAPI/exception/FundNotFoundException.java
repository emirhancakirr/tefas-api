package com.tefasfundapi.tefasFundAPI.exception;

/**
 * Exception thrown when a fund is not found.
 */
public class FundNotFoundException extends TefasException {
    public FundNotFoundException(String fundCode) {
        super("NOT_FOUND", "Fund not found: " + fundCode);
    }
}
