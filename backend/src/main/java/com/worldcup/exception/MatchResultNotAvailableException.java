package com.worldcup.exception;

/**
 * Exception thrown when attempting to perform an operation that requires match results,
 * but the match results are not yet available.
 */
public class MatchResultNotAvailableException extends WorldCupException {
    
    public MatchResultNotAvailableException(Long matchId) {
        super("MATCH_RESULT_NOT_AVAILABLE", "Match result not available for match id: " + matchId);
    }
    
    public MatchResultNotAvailableException(String message) {
        super("MATCH_RESULT_NOT_AVAILABLE", message);
    }
}

