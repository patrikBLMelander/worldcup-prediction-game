package com.worldcup.exception;

/**
 * Base exception class for all World Cup application-specific exceptions.
 * Provides structured error handling with error codes for better API responses.
 */
public abstract class WorldCupException extends RuntimeException {
    
    private final String errorCode;
    
    /**
     * Creates a new WorldCupException with the specified error code and message.
     * 
     * @param errorCode a unique error code identifying the type of error (e.g., "MATCH_NOT_FOUND")
     * @param message a descriptive error message
     */
    protected WorldCupException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a new WorldCupException with the specified error code, message, and cause.
     * 
     * @param errorCode a unique error code identifying the type of error
     * @param message a descriptive error message
     * @param cause the cause of this exception
     */
    protected WorldCupException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Returns the error code for this exception.
     * Error codes are used for programmatic error handling and API responses.
     * 
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}

