package com.worldcup.dto;

import com.worldcup.entity.MatchStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * How the members of one league predicted a single (locked) match.
 *
 * <p>Only ever built for matches that are locked (kicked off), so it can safely
 * reveal who picked what. Voters are grouped by predicted outcome; counts are the
 * list sizes. {@code noPickCount} is league members who made no prediction.
 */
public record LeaguePredictionSplitDTO(
    Long matchId,
    String homeTeam,
    String homeTeamCrest,
    String awayTeam,
    String awayTeamCrest,
    LocalDateTime matchDate,
    String group,
    MatchStatus status,
    Integer homeScore,
    Integer awayScore,
    List<Voter> homeWin,
    List<Voter> draw,
    List<Voter> awayWin,
    int noPickCount
) {
    /** A league member and whether it is the requesting user. */
    public record Voter(Long userId, String screenName, boolean isMe) {}
}
