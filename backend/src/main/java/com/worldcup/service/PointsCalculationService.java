package com.worldcup.service;

import org.springframework.stereotype.Service;

/**
 * Service responsible for calculating points for predictions.
 * 
 * Points are awarded based on prediction accuracy:
 * <ul>
 *   <li>{@value #EXACT_SCORE_POINTS} points for exact score match</li>
 *   <li>{@value #CORRECT_WINNER_POINTS} point for correct winner (non-exact score)</li>
 *   <li>{@value #WRONG_PREDICTION_POINTS} points for incorrect prediction</li>
 * </ul>
 * 
 * This service centralizes the points calculation logic to ensure consistency
 * across the application and eliminate code duplication.
 */
@Service
public final class PointsCalculationService {

    /**
     * Points awarded for predicting the exact score.
     */
    public static final int EXACT_SCORE_POINTS = 3;

    /**
     * Points awarded for predicting the correct winner (but not the exact score).
     */
    public static final int CORRECT_WINNER_POINTS = 1;

    /**
     * Points awarded for an incorrect prediction.
     */
    public static final int WRONG_PREDICTION_POINTS = 0;

    /**
     * Calculates the points awarded for a prediction based on the predicted and actual scores.
     * 
     * @param predictedHome the predicted home team score
     * @param predictedAway the predicted away team score
     * @param actualHome the actual home team score
     * @param actualAway the actual away team score
     * @return the points awarded (3 for exact score, 1 for correct winner, 0 for wrong prediction)
     */
    public int calculatePoints(int predictedHome, int predictedAway, 
                               int actualHome, int actualAway) {
        if (isExactScore(predictedHome, predictedAway, actualHome, actualAway)) {
            return EXACT_SCORE_POINTS;
        }

        if (isCorrectWinner(predictedHome, predictedAway, actualHome, actualAway)) {
            return CORRECT_WINNER_POINTS;
        }

        return WRONG_PREDICTION_POINTS;
    }

    /**
     * Checks if the predicted score exactly matches the actual score.
     * 
     * @param predictedHome the predicted home team score
     * @param predictedAway the predicted away team score
     * @param actualHome the actual home team score
     * @param actualAway the actual away team score
     * @return true if the predicted score exactly matches the actual score
     */
    public boolean isExactScore(int predictedHome, int predictedAway, 
                                int actualHome, int actualAway) {
        return predictedHome == actualHome && predictedAway == actualAway;
    }

    /**
     * Checks if the predicted winner matches the actual winner (but not exact score).
     * 
     * A correct winner is determined by:
     * <ul>
     *   <li>Home wins: predicted home score > predicted away score AND actual home score > actual away score</li>
     *   <li>Away wins: predicted away score > predicted home score AND actual away score > actual home score</li>
     *   <li>Draw: predicted scores are equal AND actual scores are equal</li>
     * </ul>
     * 
     * @param predictedHome the predicted home team score
     * @param predictedAway the predicted away team score
     * @param actualHome the actual home team score
     * @param actualAway the actual away team score
     * @return true if the predicted winner matches the actual winner (but not exact score)
     */
    public boolean isCorrectWinner(int predictedHome, int predictedAway, 
                                  int actualHome, int actualAway) {
        boolean predictedHomeWins = predictedHome > predictedAway;
        boolean predictedAwayWins = predictedAway > predictedHome;
        boolean predictedDraw = predictedHome == predictedAway;

        boolean actualHomeWins = actualHome > actualAway;
        boolean actualAwayWins = actualAway > actualHome;
        boolean actualDraw = actualHome == actualAway;

        return (predictedHomeWins && actualHomeWins) ||
               (predictedAwayWins && actualAwayWins) ||
               (predictedDraw && actualDraw);
    }
}

