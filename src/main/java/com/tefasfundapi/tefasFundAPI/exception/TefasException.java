package com.tefasfundapi.tefasFundAPI.exception;

import java.time.Instant;
import java.util.UUID;

/**
 * Base exception for all TEFAS API related errors.
 * Provides standardized error code, trace ID, and timestamp.
 */
public abstract class TefasException extends RuntimeException {
    private final String errorCode;
    private final String traceId;
    private final Instant timestamp;

    protected TefasException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.traceId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    protected TefasException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.traceId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
