package com.worldcup.dto;

import com.worldcup.entity.League;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeagueSummaryDTO {

    private Long id;
    private String name;
    private String joinCode;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime lockedAt;
    private Long ownerId;
    private String ownerScreenName;
    
    // Betting information
    private League.BettingType bettingType;
    private BigDecimal entryPrice;
    private League.PayoutStructure payoutStructure;
    private Map<Integer, BigDecimal> rankedPercentages;
}


