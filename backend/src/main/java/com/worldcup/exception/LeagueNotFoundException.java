package com.worldcup.exception;

/**
 * Exception thrown when a requested league cannot be found.
 */
public class LeagueNotFoundException extends WorldCupException {
    
    public LeagueNotFoundException(Long leagueId) {
        super("LEAGUE_NOT_FOUND", "League not found with id: " + leagueId);
    }
    
    public LeagueNotFoundException(String joinCode) {
        super("LEAGUE_NOT_FOUND", "League not found with join code: " + joinCode);
    }
    
    public LeagueNotFoundException(String message, Object... args) {
        super("LEAGUE_NOT_FOUND", String.format(message, args));
    }
}

