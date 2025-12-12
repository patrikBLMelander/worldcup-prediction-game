package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionDTO {
    private Long id;
    private Long matchId;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchDate;
    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
    private Integer points;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


