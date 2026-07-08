package com.worldcup.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Cumulative points over time for every member of a league, one data point per
 * scored match (in chronological order). Powers the "Over time" line chart.
 *
 * <p>Points are league-relative (same scoring as the leaderboard), so the final
 * cumulative value for each member equals their leaderboard total.
 */
public record LeagueScoreTimelineDTO(
    List<Member> members,
    List<TimelinePoint> points
) {
    /** A league member; {@code userId} (as string) is the series key in each point. */
    public record Member(Long userId, String name) {}

    /**
     * One match in the timeline. {@code cumulative} maps each member's userId
     * (as a string, for JSON) to their running total after this match.
     */
    public record TimelinePoint(
        Long matchId,
        String label,
        LocalDateTime matchDate,
        Map<String, Integer> cumulative
    ) {}
}
