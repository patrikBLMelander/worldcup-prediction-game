package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDTO {
    private Long userId;
    private String email;
    private String screenName;
    private Integer totalPoints;
    private Integer predictionCount;
    
    // Prize information (for Flat Stakes leagues)
    private BigDecimal prizeAmount;
    private Integer rank; // 1-based rank (1st, 2nd, 3rd, etc.)
}

