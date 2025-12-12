package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDTO {
    private Long userId;
    private String email;
    private String screenName;
    private Integer totalPoints;
    private Integer predictionCount;
}

