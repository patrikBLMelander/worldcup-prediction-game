package com.worldcup.exception;

/**
 * Exception thrown when attempting to join a league that is locked (no longer accepting new members).
 */
public class LeagueLockedException extends WorldCupException {
    
    public LeagueLockedException(Long leagueId) {
        super("LEAGUE_LOCKED", "League is locked for new members. League id: " + leagueId);
    }
    
    public LeagueLockedException(String message) {
        super("LEAGUE_LOCKED", message);
    }
}

