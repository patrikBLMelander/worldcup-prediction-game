package com.worldcup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePredictionRequest {
    @NotNull(message = "Match ID is required")
    private Long matchId;

    @NotNull(message = "Predicted home score is required")
    private Integer predictedHomeScore;

    @NotNull(message = "Predicted away score is required")
    private Integer predictedAwayScore;
}


