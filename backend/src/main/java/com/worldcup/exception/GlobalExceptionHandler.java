package com.worldcup.exception;

import com.worldcup.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles all custom WorldCupException instances.
     * Returns structured error responses with error codes.
     */
    @ExceptionHandler(WorldCupException.class)
    public ResponseEntity<ErrorResponse> handleWorldCupException(
            WorldCupException ex, 
            HttpServletRequest request) {
        log.warn("Business exception: {} - {} at {}", 
                ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ex.getErrorCode(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles IllegalArgumentException for backward compatibility.
     * These should eventually be replaced with custom exceptions.
     * 
     * @deprecated Use custom exceptions (WorldCupException subclasses) instead
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        log.warn("IllegalArgumentException at {}: {}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_ARGUMENT",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles IllegalStateException for backward compatibility.
     * These should eventually be replaced with custom exceptions.
     * 
     * @deprecated Use custom exceptions (WorldCupException subclasses) instead
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, 
            HttpServletRequest request) {
        log.warn("IllegalStateException at {}: {}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_STATE",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles all other unexpected exceptions.
     * Logs the full exception for debugging but returns a generic error to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        log.error("Unexpected error at {}: ", request.getRequestURI(), ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        request.getRequestURI()
                ));
    }
}

