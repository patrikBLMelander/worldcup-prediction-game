package com.worldcup.dto;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Match information.
 * Immutable record representing a football match.
 *
 * <p>{@code homeScore}/{@code awayScore} are the regulation (90-minute) result,
 * which is the outcome that counts for scoring. The extra-time and penalty fields
 * are display-only annotations for knockout matches decided after 90 minutes.
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
    Integer awayScore,
    String duration, // REGULAR / EXTRA_TIME / PENALTY_SHOOTOUT
    Integer extraTimeHome,
    Integer extraTimeAway,
    Integer penaltiesHome,
    Integer penaltiesAway
) {
    /**
     * Map a Match entity to its DTO. Single source of truth for the mapping.
     */
    public static MatchDTO from(Match match) {
        return new MatchDTO(
            match.getId(),
            match.getHomeTeam(),
            match.getHomeTeamCrest(),
            match.getAwayTeam(),
            match.getAwayTeamCrest(),
            match.getMatchDate(),
            match.getVenue(),
            match.getGroup(),
            match.getStatus(),
            match.getHomeScore(),
            match.getAwayScore(),
            match.getDuration(),
            match.getExtraTimeHome(),
            match.getExtraTimeAway(),
            match.getPenaltiesHome(),
            match.getPenaltiesAway()
        );
    }
}
