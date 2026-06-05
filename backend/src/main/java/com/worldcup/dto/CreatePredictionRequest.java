package com.worldcup.dto;

import com.worldcup.entity.PredictionOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePredictionRequest {
    @NotNull(message = "Match ID is required")
    private Long matchId;

    @NotNull(message = "Predicted outcome is required")
    private PredictionOutcome predictedOutcome;
}
