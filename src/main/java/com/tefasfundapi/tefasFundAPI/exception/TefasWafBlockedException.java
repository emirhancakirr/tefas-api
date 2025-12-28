package com.tefasfundapi.tefasFundAPI.exception;

/**
 * Exception thrown when TEFAS WAF blocks the request.
 */
public class TefasWafBlockedException extends TefasClientException {
    public TefasWafBlockedException(String responsePreview) {
        super("TEFAS WAF blocked the request. Response: " + responsePreview);
    }

    public TefasWafBlockedException(String responsePreview, Throwable cause) {
        super("TEFAS WAF blocked the request. Response: " + responsePreview, cause);
    }
}
