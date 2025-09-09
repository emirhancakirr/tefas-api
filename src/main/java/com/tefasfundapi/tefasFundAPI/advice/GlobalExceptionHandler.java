package com.tefasfundapi.tefasFundAPI.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Validation hataları (ör. @NotNull parametreler)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + " " + error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("error", "BAD_REQUEST");
        body.put("message", "Validation failed");
        body.put("details", details);
        body.put("traceId", UUID.randomUUID().toString());
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Genel runtime hataları
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "INTERNAL_ERROR");
        body.put("message", ex.getMessage());
        body.put("details", List.of());
        body.put("traceId", UUID.randomUUID().toString());
        body.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}