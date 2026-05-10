package com.worldcup.repository;

import com.worldcup.entity.League;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {

    @EntityGraph(attributePaths = {"owner"})
    Optional<League> findByJoinCode(String joinCode);
    
    /**
     * Find league by ID with owner eagerly fetched.
     * Used to avoid LazyInitializationException when accessing owner.
     */
    @EntityGraph(attributePaths = {"owner"})
    @Override
    Optional<League> findById(Long id);
    
    /**
     * Find all leagues that have finished (endDate <= now) and are not hidden.
     * Used for processing achievements and other post-league operations.
     */
    @Query("SELECT l FROM League l WHERE l.endDate <= :now AND (l.hidden IS NULL OR l.hidden = false)")
    List<League> findFinishedLeagues(@Param("now") LocalDateTime now);

    // Hides every existing league and resets the achievements-processed flag
    // so they won't appear in user-facing lists. Called by the cleanup endpoint.
    @Modifying
    @Query(value = "UPDATE leagues SET hidden = TRUE, achievements_processed = FALSE",
            nativeQuery = true)
    int hideAllAndResetAchievementsProcessed();
}


