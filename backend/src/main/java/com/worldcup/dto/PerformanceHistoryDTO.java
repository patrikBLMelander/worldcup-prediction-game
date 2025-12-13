package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceHistoryDTO {
    private Long matchId;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchDate;
    private Integer predictedHomeScore;
    private Integer predictedAwayScore;
    private Integer actualHomeScore;
    private Integer actualAwayScore;
    private Integer points;
    private String resultType; // "EXACT", "CORRECT_WINNER", "WRONG"
    private Integer cumulativePoints; // Running total of points
}

