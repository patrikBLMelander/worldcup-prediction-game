package com.worldcup.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}


