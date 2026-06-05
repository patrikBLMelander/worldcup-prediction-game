package com.worldcup.entity;

/**
 * The outcome a user predicts for a match (Copabet-style scoring).
 * Users pick only who wins (or a draw), not the exact score.
 */
public enum PredictionOutcome {
    HOME_WIN,
    DRAW,
    AWAY_WIN;

    /**
     * Derives the outcome implied by a pair of scores.
     */
    public static PredictionOutcome fromScores(int homeScore, int awayScore) {
        if (homeScore > awayScore) {
            return HOME_WIN;
        }
        if (awayScore > homeScore) {
            return AWAY_WIN;
        }
        return DRAW;
    }
}
