package com.worldcup.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Structured error response for API error handling.
 * Immutable record representing an error with error code, message, path, and timestamp.
 * 
 * Backward compatible: includes both "message" and "error" fields for frontend compatibility.
 */
public record ErrorResponse(
    String errorCode,
    String message,
    String path,
    LocalDateTime timestamp
) {
    /**
     * Creates an ErrorResponse with the current timestamp.
     * 
     * @param errorCode the error code
     * @param message the error message
     * @param path the request path where the error occurred
     */
    public ErrorResponse(String errorCode, String message, String path) {
        this(errorCode, message, path, LocalDateTime.now());
    }
    
    /**
     * Creates an ErrorResponse with error code and message only (no path).
     * Useful for backward compatibility or when path is not available.
     * 
     * @param errorCode the error code
     * @param message the error message
     */
    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, null, LocalDateTime.now());
    }
    
    /**
     * Backward compatibility: returns message as "error" for frontend compatibility.
     * The frontend expects error.response?.data?.error, so we provide this alias.
     */
    @JsonProperty("error")
    public String error() {
        return message;
    }
}

