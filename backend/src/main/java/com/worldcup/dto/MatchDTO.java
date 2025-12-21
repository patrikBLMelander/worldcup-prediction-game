package com.worldcup.dto;

import com.worldcup.entity.MatchStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Match information.
 * Immutable record representing a football match.
 */
public record MatchDTO(
    Long id,
    String homeTeam,
    String homeTeamCrest, // Team logo/crest URL
    String awayTeam,
    String awayTeamCrest, // Team logo/crest URL
    LocalDateTime matchDate,
    String venue,
    String group,
    MatchStatus status,
    Integer homeScore,
    Integer awayScore
) {}


