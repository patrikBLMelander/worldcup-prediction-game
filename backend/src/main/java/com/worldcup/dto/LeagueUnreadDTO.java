package com.worldcup.dto;

/** Unread chat count for a single league, used for the chat badge. */
public record LeagueUnreadDTO(
    Long leagueId,
    String leagueName,
    long unreadCount
) {}
