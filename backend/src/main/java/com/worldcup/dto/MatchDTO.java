package com.worldcup.dto;

import com.worldcup.entity.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    private Long id;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchDate;
    private String venue;
    private String group;
    private MatchStatus status;
    private Integer homeScore;
    private Integer awayScore;
}


