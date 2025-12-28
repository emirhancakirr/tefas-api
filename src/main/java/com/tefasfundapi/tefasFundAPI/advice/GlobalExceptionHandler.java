package com.tefasfundapi.tefasFundAPI.advice;

import com.tefasfundapi.tefasFundAPI.dto.ErrorResponse;
import com.tefasfundapi.tefasFundAPI.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST API errors.
 * Provides standardized error responses with proper HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        // ==================== Custom TEFAS Exceptions ====================

        @ExceptionHandler(FundNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(FundNotFoundException ex, HttpServletRequest request) {
                log.warn("Resource not found: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ErrorResponse.from(ex, request.getRequestURI()));
        }

        @ExceptionHandler(InvalidDateRangeException.class)
        public ResponseEntity<ErrorResponse> handleBadRequest(InvalidDateRangeException ex,
                        HttpServletRequest request) {
                log.warn("Invalid request: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.from(ex, request.getRequestURI()));
        }

        @ExceptionHandler(TefasTimeoutException.class)
        public ResponseEntity<ErrorResponse> handleTimeout(TefasTimeoutException ex, HttpServletRequest request) {
                log.error("Timeout error: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                                .body(ErrorResponse.from(ex, request.getRequestURI()));
        }

        @ExceptionHandler(TefasWafBlockedException.class)
        public ResponseEntity<ErrorResponse> handleWafBlocked(TefasWafBlockedException ex, HttpServletRequest request) {
                log.error("WAF blocked request: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ErrorResponse.from(ex, request.getRequestURI()));
        }

        @ExceptionHandler(TefasClientException.class)
        public ResponseEntity<ErrorResponse> handleClientError(TefasClientException ex, HttpServletRequest request) {
                log.error("Client error: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body(ErrorResponse.from(ex, request.getRequestURI()));
        }

        @ExceptionHandler(TefasParseException.class)
        public ResponseEntity<ErrorResponse> handleParseError(TefasParseException ex, HttpServletRequest request) {
                log.error("Parse error: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ErrorResponse.from(ex, request.getRequestURI()));
        }

        @ExceptionHandler(TefasException.class)
        public ResponseEntity<ErrorResponse> handleTefasException(TefasException ex, HttpServletRequest request) {
                log.error("TEFAS error: {}", ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.from(ex, request.getRequestURI()));
        }

        // ==================== Validation Errors ====================

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                List<String> details = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + " " + error.getDefaultMessage())
                                .collect(Collectors.toList());

                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("VALIDATION_ERROR")
                                .message("Validation failed")
                                .details(details)
                                .path(request.getRequestURI())
                                .build();

                log.warn("Validation error: {}", details);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // ==================== Generic Exceptions ====================

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest request) {
                log.error("Unexpected runtime error: {}", ex.getMessage(), ex);

                List<String> details = List.of(ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("INTERNAL_ERROR")
                                .message("An unexpected error occurred")
                                .details(details)
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
                log.error("Unhandled exception: {}", ex.getMessage(), ex);

                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("INTERNAL_ERROR")
                                .message("An unexpected error occurred")
                                .path(request.getRequestURI())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        // ==================== Spring Framework Exceptions ====================

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ErrorResponse> handleMissingParameter(
                        MissingServletRequestParameterException ex, HttpServletRequest request) {
                String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("BAD_REQUEST")
                                .message(message)
                                .details(List.of("Parameter: " + ex.getParameterName(),
                                                "Type: " + ex.getParameterType()))
                                .path(request.getRequestURI())
                                .build();

                log.warn("Missing parameter: {}", ex.getParameterName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
                String message = String.format("Parameter '%s' has invalid type. Expected: %s, Got: %s",
                                ex.getName(),
                                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                                ex.getValue() != null ? ex.getValue().getClass().getSimpleName() : "null");

                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("BAD_REQUEST")
                                .message(message)
                                .details(List.of("Parameter: " + ex.getName(), "Value: " + ex.getValue()))
                                .path(request.getRequestURI())
                                .build();

                log.warn("Type mismatch for parameter: {}", ex.getName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(DateTimeParseException.class)
        public ResponseEntity<ErrorResponse> handleDateParseError(
                        DateTimeParseException ex, HttpServletRequest request) {
                String message = String.format("Invalid date format. Expected: YYYY-MM-DD, Got: '%s'",
                                ex.getParsedString());

                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("BAD_REQUEST")
                                .message(message)
                                .details(List.of("Parsed string: " + ex.getParsedString(),
                                                "Error index: " + ex.getErrorIndex()))
                                .path(request.getRequestURI())
                                .build();

                log.warn("Date parse error: {}", ex.getParsedString());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
                String message = String.format("Method '%s' is not supported. Supported methods: %s",
                                ex.getMethod(), String.join(", ", ex.getSupportedMethods()));

                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("METHOD_NOT_ALLOWED")
                                .message(message)
                                .path(request.getRequestURI())
                                .build();

                log.warn("Method not supported: {}", ex.getMethod());
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
        }

        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
                ErrorResponse error = ErrorResponse.builder()
                                .errorCode("NOT_FOUND")
                                .message("No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL())
                                .path(request.getRequestURI())
                                .build();

                log.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
}