package com.worldcup.repository;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStatus(MatchStatus status);
    List<Match> findByMatchDateBetween(LocalDateTime start, LocalDateTime end);
    List<Match> findByGroup(String group);
    List<Match> findByStatusOrderByMatchDateAsc(MatchStatus status);
    List<Match> findByStatusAndMatchDateAfter(MatchStatus status, LocalDateTime date);

    // Optimized query to only fetch matches with external API IDs (for API sync)
    // This avoids loading all matches into memory
    @Query("SELECT m FROM Match m WHERE m.externalApiId IS NOT NULL AND m.externalApiId != ''")
    List<Match> findAllWithExternalApiId();

    // Allow-list of World Cup stage labels produced by FootballApiService.stageLabel().
    // Used as the "keep" set for cleanup so we don't have to enumerate every
    // possible foreign stage label (Premier League "REGULAR_SEASON", etc.).
    String WC_GROUP_LIKE = "Group %";
    String[] WC_KNOCKOUT_LABELS = {
        "Round of 32", "Round of 16", "Quarter-Final",
        "Semi-Final", "Third-Place Play-off", "Final"
    };

    // Counts API-sourced matches with a World Cup stage label. The cleanup
    // endpoint refuses to run if this is too low (i.e. WC sync hasn't completed).
    @Query(value = "SELECT COUNT(*) FROM matches " +
            "WHERE external_api_id IS NOT NULL AND external_api_id <> '' " +
            "AND (match_group LIKE 'Group %' " +
            "     OR match_group IN ('Round of 32','Round of 16','Quarter-Final'," +
            "                        'Semi-Final','Third-Place Play-off','Final'))",
            nativeQuery = true)
    long countMatchesToKeep();

    // Deletes every match that isn't an API-sourced World Cup fixture.
    // Catches: PL matches (group 'REGULAR_SEASON' or 'Premier League'),
    // mock-seeded matches (external_api_id is null), and any other foreign
    // competition residue. Predictions must be deleted first.
    @Modifying
    @Query(value = "DELETE FROM matches WHERE NOT (" +
            "  external_api_id IS NOT NULL AND external_api_id <> '' " +
            "  AND (match_group LIKE 'Group %' " +
            "       OR match_group IN ('Round of 32','Round of 16','Quarter-Final'," +
            "                          'Semi-Final','Third-Place Play-off','Final'))" +
            ")",
            nativeQuery = true)
    int deleteNonWorldCupMatches();
}


