package com.worldcup.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User Profile information.
 * Immutable record representing a user's profile with statistics.
 */
public record UserProfileDTO(
    Long id,
    String email,
    String screenName,
    String role,
    Integer totalPoints,
    Integer predictionCount,
    LocalDateTime createdAt
) {}

