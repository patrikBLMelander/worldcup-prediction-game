package com.worldcup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMatchResultRequest {
    @NotNull(message = "Home score is required")
    private Integer homeScore;

    @NotNull(message = "Away score is required")
    private Integer awayScore;
}


