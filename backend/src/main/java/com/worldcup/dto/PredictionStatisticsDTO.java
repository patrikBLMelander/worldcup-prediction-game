package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionStatisticsDTO {
    private int totalPredictions;
    private int correctPredictions; // outcome matched the actual result
    private int wrongPredictions;   // outcome did not match
    private double accuracyPercentage; // correctPredictions / totalPredictions * 100
    private int totalPoints; // global-pool points
}

