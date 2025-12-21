package com.worldcup.exception;

/**
 * Exception thrown when a requested user cannot be found.
 */
public class UserNotFoundException extends WorldCupException {
    
    public UserNotFoundException(Long userId) {
        super("USER_NOT_FOUND", "User not found with id: " + userId);
    }
    
    public UserNotFoundException(String email) {
        super("USER_NOT_FOUND", "User not found with email: " + email);
    }
    
    public UserNotFoundException(String message, Object... args) {
        super("USER_NOT_FOUND", String.format(message, args));
    }
}

