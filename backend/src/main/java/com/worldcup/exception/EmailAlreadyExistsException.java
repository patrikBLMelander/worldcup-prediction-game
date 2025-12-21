package com.worldcup.exception;

/**
 * Exception thrown when attempting to create a user with an email that already exists.
 */
public class EmailAlreadyExistsException extends WorldCupException {
    
    public EmailAlreadyExistsException(String email) {
        super("EMAIL_ALREADY_EXISTS", "Email already exists: " + email);
    }
}

