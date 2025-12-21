package com.worldcup.exception;

/**
 * Exception thrown when password validation fails (e.g., incorrect current password).
 */
public class InvalidPasswordException extends WorldCupException {
    
    public InvalidPasswordException(String message) {
        super("INVALID_PASSWORD", message);
    }
    
    public InvalidPasswordException() {
        super("INVALID_PASSWORD", "Current password is incorrect");
    }
}

