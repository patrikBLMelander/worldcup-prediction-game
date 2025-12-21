package com.worldcup.exception;

import com.worldcup.entity.MatchStatus;

/**
 * Exception thrown when a match is in an invalid state for the requested operation.
 * For example, attempting to calculate points for a match that is not FINISHED.
 */
public class InvalidMatchStateException extends WorldCupException {
    
    public InvalidMatchStateException(MatchStatus currentStatus, String operation) {
        super("INVALID_MATCH_STATE", 
            String.format("Cannot %s for match with status: %s", operation, currentStatus));
    }
    
    public InvalidMatchStateException(String message) {
        super("INVALID_MATCH_STATE", message);
    }
}

