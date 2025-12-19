package com.worldcup.dto;

import com.worldcup.entity.League;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class CreateLeagueRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    /**
     * Start of the scoring window (inclusive).
     * Only matches with kick-off >= startDate are counted.
     */
    @NotNull
    private LocalDateTime startDate;

    /**
     * End of the scoring window (inclusive).
     * Only matches with kick-off <= endDate are counted.
     */
    @NotNull
    @Future
    private LocalDateTime endDate;

    /**
     * Betting type for this league.
     * FLAT_STAKES: Everyone pays the same entry price.
     * CUSTOM_STAKES: Each player sets their own stake (future feature).
     */
    private League.BettingType bettingType;

    /**
     * Entry price for Flat Stakes leagues.
     * Required when bettingType is FLAT_STAKES.
     */
    @DecimalMin(value = "0.01", message = "Entry price must be at least 0.01")
    private BigDecimal entryPrice;

    /**
     * Payout structure for Flat Stakes leagues.
     * WINNER_TAKES_ALL: 100% to 1st place.
     * RANKED: Percentage-based distribution (see rankedPercentages).
     */
    private League.PayoutStructure payoutStructure;

    /**
     * Ranked payout percentages for Flat Stakes leagues.
     * Format: {1: 0.60, 2: 0.30, 3: 0.10}
     * Only used when payoutStructure is RANKED.
     * Percentages should sum to 1.0 (100%).
     */
    private Map<Integer, BigDecimal> rankedPercentages;
}


