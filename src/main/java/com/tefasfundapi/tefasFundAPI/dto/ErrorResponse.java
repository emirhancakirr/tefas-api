package com.tefasfundapi.tefasFundAPI.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tefasfundapi.tefasFundAPI.exception.TefasException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard error response DTO for all API errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String errorCode;
    private String message;
    private List<String> details;
    private String traceId;
    private Instant timestamp;
    private String path;
    private Map<String, Object> metadata;

    public ErrorResponse() {
    }

    public ErrorResponse(
            String errorCode,
            String message,
            List<String> details,
            String traceId,
            Instant timestamp,
            String path,
            Map<String, Object> metadata) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.traceId = traceId;
        this.timestamp = timestamp;
        this.path = path;
        this.metadata = metadata;
    }

    /**
     * Creates ErrorResponse from TefasException.
     */
    public static ErrorResponse from(TefasException ex) {
        return ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .traceId(ex.getTraceId())
                .timestamp(ex.getTimestamp())
                .build();
    }

    /**
     * Creates ErrorResponse from TefasException with request path.
     */
    public static ErrorResponse from(TefasException ex, String path) {
        return ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .traceId(ex.getTraceId())
                .timestamp(ex.getTimestamp())
                .path(path)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class Builder {
        private String errorCode;
        private String message;
        private List<String> details;
        private String traceId;
        private Instant timestamp;
        private String path;
        private Map<String, Object> metadata;

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(List<String> details) {
            this.details = details;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(
                    errorCode,
                    message,
                    details,
                    traceId,
                    timestamp,
                    path,
                    metadata);
        }
    }
}
