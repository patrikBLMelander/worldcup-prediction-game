package com.worldcup.exception;

/**
 * Exception thrown when a requested match cannot be found.
 */
public class MatchNotFoundException extends WorldCupException {
    
    public MatchNotFoundException(Long matchId) {
        super("MATCH_NOT_FOUND", "Match not found with id: " + matchId);
    }
    
    public MatchNotFoundException(String message) {
        super("MATCH_NOT_FOUND", message);
    }
}

