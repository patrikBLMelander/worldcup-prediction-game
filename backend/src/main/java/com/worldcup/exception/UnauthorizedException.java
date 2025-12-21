package com.worldcup.exception;

public class UnauthorizedException extends WorldCupException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super("UNAUTHORIZED", message, cause);
    }
}

