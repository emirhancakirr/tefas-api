package com.tefasfundapi.tefasFundAPI.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tefasfundapi.tefasFundAPI.exception.TefasException;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard error response DTO for all API errors.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String errorCode;
    private String message;
    private List<String> details;
    private String traceId;
    private Instant timestamp;
    private String path;
    private Map<String, Object> metadata;

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
        ErrorResponse response = from(ex);
        response.setPath(path);
        return response;
    }
}
