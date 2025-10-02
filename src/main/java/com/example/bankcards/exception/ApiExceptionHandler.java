package com.example.bankcards.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.toList());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors.toString(), req);
    }

    @ExceptionHandler({CardOperationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleCardOperationException(RuntimeException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler({AccessDeniedException.class, NotCardOwnerException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(RuntimeException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(CardNotActiveException.class)
    public ResponseEntity<Map<String,Object>> handleCardNotActive(CardNotActiveException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler({InsufficientFundsException.class, UserAlreadyExistsException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex, HttpServletRequest req) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", request.getRequestURI()
        );
        return new ResponseEntity<>(body, status);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message,
                "path", request.getRequestURI()
        );
        return new ResponseEntity<>(body, status);
    }
}
