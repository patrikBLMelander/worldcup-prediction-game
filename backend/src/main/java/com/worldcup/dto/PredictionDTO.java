package com.worldcup.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Prediction information.
 * Immutable record representing a user's prediction for a match.
 */
public record PredictionDTO(
    Long id,
    Long matchId,
    String homeTeam,
    String awayTeam,
    LocalDateTime matchDate,
    Integer predictedHomeScore,
    Integer predictedAwayScore,
    Integer points,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}


