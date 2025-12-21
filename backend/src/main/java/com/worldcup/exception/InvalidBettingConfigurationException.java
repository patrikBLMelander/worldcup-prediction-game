package com.worldcup.exception;

/**
 * Exception thrown when league betting configuration is invalid.
 * Used for validation errors related to entry price, payout structure, and ranked percentages.
 */
public class InvalidBettingConfigurationException extends WorldCupException {
    
    public InvalidBettingConfigurationException(String message) {
        super("INVALID_BETTING_CONFIG", message);
    }
    
    public InvalidBettingConfigurationException(String message, Object... args) {
        super("INVALID_BETTING_CONFIG", String.format(message, args));
    }
}

