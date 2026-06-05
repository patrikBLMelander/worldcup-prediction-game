package com.worldcup.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "predictions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "match_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /**
     * The predicted match outcome (Copabet-style). This is the source of truth
     * for scoring. May be null only for legacy rows pending backfill.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_outcome", length = 20)
    private PredictionOutcome predictedOutcome;

    // Legacy exact-score fields. No longer collected from users; kept nullable
    // so historical rows remain readable and the outcome can be derived from them.
    @Column
    private Integer predictedHomeScore;

    @Column
    private Integer predictedAwayScore;

    /**
     * Points for this prediction in the GLOBAL pool (all players who predicted
     * this match). Stable once the match finishes because predictions are locked
     * at kickoff. Per-league points are computed separately at read time.
     */
    @Column
    private Integer points; // Calculated after match finishes

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

