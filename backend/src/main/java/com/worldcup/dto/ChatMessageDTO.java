package com.worldcup.dto;

import java.time.LocalDateTime;

/**
 * A league chat message for the client. {@code userId} lets the client decide
 * which messages are the current user's own.
 */
public record ChatMessageDTO(
    Long id,
    Long leagueId,
    Long userId,
    String screenName,
    String content,
    LocalDateTime createdAt
) {}
