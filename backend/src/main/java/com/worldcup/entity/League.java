package com.worldcup.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(
    name = "leagues",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "join_code")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Short, user-shareable code used to join this league.
     * We keep it unique to avoid ambiguity when joining.
     */
    @NotBlank
    @Size(max = 32)
    @Column(name = "join_code", nullable = false, length = 32, unique = true)
    private String joinCode;

    /**
     * Owning user (creator of the league).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Start of the scoring window (inclusive).
     * Only matches with kick-off >= startDate are counted in this league.
     */
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * End of the scoring window (inclusive).
     * Only matches with kick-off <= endDate are counted in this league.
     */
    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    /**
     * When the league was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the league membership was locked (no more joins).
     * For the MVP this will typically be set at or before first kick-off.
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /**
     * Whether leaderboard achievements have been processed for this league.
     * Set to true after achievements are awarded when league finishes.
     * Nullable to allow migration of existing data; defaults to false in application logic.
     */
    @Column(name = "achievements_processed")
    private Boolean achievementsProcessed = false;

    /**
     * Betting type for this league.
     * FLAT_STAKES: Everyone pays the same entry price.
     * CUSTOM_STAKES: Each player sets their own stake (future feature).
     */
    @Column(name = "betting_type", length = 20)
    @Enumerated(EnumType.STRING)
    private BettingType bettingType;

    /**
     * Entry price for Flat Stakes leagues.
     * Required when bettingType is FLAT_STAKES.
     */
    @Column(name = "entry_price", precision = 10, scale = 2)
    private BigDecimal entryPrice;

    /**
     * Payout structure for Flat Stakes leagues.
     * WINNER_TAKES_ALL: 100% to 1st place.
     * RANKED: Percentage-based distribution (see rankedPercentages).
     */
    @Column(name = "payout_structure", length = 20)
    @Enumerated(EnumType.STRING)
    private PayoutStructure payoutStructure;

    /**
     * Ranked payout percentages for Flat Stakes leagues.
     * Stored as JSON: {"1": 0.60, "2": 0.30, "3": 0.10}
     * Only used when payoutStructure is RANKED.
     */
    @Column(name = "ranked_percentages", columnDefinition = "TEXT")
    @Convert(converter = RankedPercentagesConverter.class)
    private Map<Integer, BigDecimal> rankedPercentages;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum BettingType {
        FLAT_STAKES,
        CUSTOM_STAKES
    }

    public enum PayoutStructure {
        WINNER_TAKES_ALL,
        RANKED
    }
}


