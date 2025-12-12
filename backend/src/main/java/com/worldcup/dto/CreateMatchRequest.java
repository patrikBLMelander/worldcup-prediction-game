package com.worldcup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateMatchRequest {
    @NotBlank(message = "Home team is required")
    private String homeTeam;

    @NotBlank(message = "Away team is required")
    private String awayTeam;

    @NotNull(message = "Match date is required")
    private LocalDateTime matchDate;

    private String venue;
    private String group;
}


