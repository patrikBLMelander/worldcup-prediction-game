package com.worldcup.dto;

import com.worldcup.entity.PredictionOutcome;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinishedPredictionDTO {
    private Long matchId;
    private String homeTeam;
    private String homeTeamCrest;
    private String awayTeam;
    private String awayTeamCrest;
    private LocalDateTime matchDate;
    private String venue;
    private String group;
    private PredictionOutcome predictedOutcome;
    private Integer actualHomeScore;
    private Integer actualAwayScore;
    private Integer points;
    private String resultType; // "CORRECT", "WRONG"
    private String matchStatus; // "SCHEDULED", "LIVE", "FINISHED", "CANCELLED"
}

