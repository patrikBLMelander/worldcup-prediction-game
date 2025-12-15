package com.worldcup.dto;

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
    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
    private Integer actualHomeScore;
    private Integer actualAwayScore;
    private Integer points;
    private String resultType; // "EXACT", "CORRECT_WINNER", "WRONG"
    private String matchStatus; // "SCHEDULED", "LIVE", "FINISHED", "CANCELLED"
}

