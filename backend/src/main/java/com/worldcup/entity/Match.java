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

    @NotBlank
    @Column(nullable = false, length = 100)
    private String awayTeam;

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
    private Integer homeScore; // Actual result

    @Column
    private Integer awayScore; // Actual result

    @Column(name = "external_api_id", length = 100)
    private String externalApiId; // ID from external API (e.g., Football-Data.org, API-Football)

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Prediction> predictions = new ArrayList<>();
}

