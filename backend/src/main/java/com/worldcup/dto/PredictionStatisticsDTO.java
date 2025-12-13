package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionStatisticsDTO {
    private int totalPredictions;
    private int exactScores; // 3 points
    private int correctWinners; // 1 point
    private int wrongPredictions; // 0 points
    private double accuracyPercentage; // Percentage of correct predictions (exact + correct winner)
    private int totalPoints;
}

