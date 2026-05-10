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

    // Counts matches we want to keep (API-sourced and not Premier League).
    // Used by the cleanup endpoint to refuse running before WC fixtures are synced.
    @Query(value = "SELECT COUNT(*) FROM matches " +
            "WHERE external_api_id IS NOT NULL AND external_api_id <> '' " +
            "AND (match_group IS NULL OR match_group <> 'Premier League')",
            nativeQuery = true)
    long countMatchesToKeep();

    // Deletes Premier League matches and any mock-seeded matches lacking
    // an external API id. Predictions must be deleted first.
    @Modifying
    @Query(value = "DELETE FROM matches " +
            "WHERE match_group = 'Premier League' " +
            "OR external_api_id IS NULL OR external_api_id = ''",
            nativeQuery = true)
    int deleteTestAndPremierLeagueMatches();
}


