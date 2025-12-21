package com.worldcup.exception;

import com.worldcup.entity.MatchStatus;

/**
 * Exception thrown when attempting to create or update a prediction for a match
 * that is no longer open for predictions (e.g., match is LIVE or FINISHED).
 */
public class PredictionLockedException extends WorldCupException {
    
    public PredictionLockedException(MatchStatus currentStatus) {
        super("PREDICTION_LOCKED", 
            "Cannot make predictions for matches that are not scheduled. Match status: " + currentStatus);
    }
    
    public PredictionLockedException(String message) {
        super("PREDICTION_LOCKED", message);
    }
}

