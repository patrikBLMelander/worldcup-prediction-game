package com.worldcup.service;

import com.worldcup.entity.PredictionOutcome;
import org.springframework.stereotype.Service;

/**
 * Copabet-style relative scoring.
 *
 * <p>A correct outcome is worth more the fewer people got it right. Within a
 * scope (a league, or the global pool of all players who predicted the match):
 * <ul>
 *   <li>only one correct predictor → full {@code gameValue}</li>
 *   <li>everyone correct → {@code gameValue / 10}</li>
 *   <li>linear in between</li>
 * </ul>
 * A wrong prediction scores 0.
 *
 * <p>The game value grows through the knockout rounds (group 100 → final 200).
 */
@Service
public final class RelativeScoringService {

    public static final int GROUP_STAGE_VALUE = 100;

    /**
     * Game value for a match, keyed off its stage label ({@code Match.group}).
     */
    public int gameValue(String matchGroup) {
        if (matchGroup == null || matchGroup.startsWith("Group ")) {
            return GROUP_STAGE_VALUE;
        }
        return switch (matchGroup) {
            case "Round of 32" -> 120;
            case "Round of 16" -> 140;
            case "Quarter-Final" -> 160;
            case "Semi-Final" -> 180;
            case "Third-Place Play-off" -> 190;
            case "Final" -> 200;
            default -> GROUP_STAGE_VALUE;
        };
    }

    /**
     * The actual outcome of a finished match.
     */
    public PredictionOutcome actualOutcome(int homeScore, int awayScore) {
        return PredictionOutcome.fromScores(homeScore, awayScore);
    }

    /**
     * Points awarded to a CORRECT predictor.
     *
     * @param gameValue      the round's game value (max possible)
     * @param correctCount   how many predictors in scope picked the actual outcome (>= 1)
     * @param predictorCount how many predictors in scope made a prediction (>= correctCount)
     * @return points for each correct predictor; {@code gameValue} when alone,
     *         down to {@code gameValue / 10} when everyone was correct
     */
    public int correctPredictionPoints(int gameValue, int correctCount, int predictorCount) {
        // Alone correct, or a single-predictor pool → maximum value.
        if (predictorCount <= 1 || correctCount <= 1) {
            return gameValue;
        }
        double minValue = gameValue / 10.0;
        double fraction = (double) (correctCount - 1) / (predictorCount - 1);
        double points = gameValue - (gameValue - minValue) * fraction;
        return (int) Math.round(points);
    }
}
