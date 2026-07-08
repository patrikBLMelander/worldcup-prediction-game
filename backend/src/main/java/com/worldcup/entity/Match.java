package com.worldcup.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.worldcup.config.MatchEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@EntityListeners(MatchEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String homeTeam;

    @Column(length = 500)
    private String homeTeamCrest; // Team logo/crest URL

    @NotBlank
    @Column(nullable = false, length = 100)
    private String awayTeam;

    @Column(length = 500)
    private String awayTeamCrest; // Team logo/crest URL

    @NotNull
    @Column(nullable = false)
    private LocalDateTime matchDate;

    @Column(length = 200)
    private String venue;

    @Column(name = "match_group", length = 50)
    private String group; // e.g., "Group A", "Round of 16", "Quarter-Final", etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column
    private Integer homeScore; // Regulation (90-minute) result - source of truth for scoring

    @Column
    private Integer awayScore; // Regulation (90-minute) result - source of truth for scoring

    /**
     * How the match was decided: REGULAR, EXTRA_TIME, or PENALTY_SHOOTOUT
     * (from football-data.org score.duration). Null for matches without a result.
     * Scoring always uses the regulation score above; these fields are display-only.
     */
    @Column(length = 30)
    private String duration;

    // Goals scored in extra time (null unless the match went to extra time).
    @Column
    private Integer extraTimeHome;

    @Column
    private Integer extraTimeAway;

    // Penalty shootout tally (null unless the match went to penalties).
    @Column
    private Integer penaltiesHome;

    @Column
    private Integer penaltiesAway;

    @Column(name = "external_api_id", length = 100)
    private String externalApiId; // ID from external API (e.g., Football-Data.org, API-Football)

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Prediction> predictions = new ArrayList<>();
}

