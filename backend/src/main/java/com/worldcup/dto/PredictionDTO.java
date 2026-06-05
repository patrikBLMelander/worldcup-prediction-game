package com.worldcup.dto;

import com.worldcup.entity.PredictionOutcome;

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
    PredictionOutcome predictedOutcome,
    Integer points,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
