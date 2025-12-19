package com.worldcup.repository;

import com.worldcup.entity.League;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {

    @EntityGraph(attributePaths = {"owner"})
    Optional<League> findByJoinCode(String joinCode);
    
    /**
     * Find all leagues that have finished (endDate <= now)
     */
    @Query("SELECT l FROM League l WHERE l.endDate <= :now")
    List<League> findFinishedLeagues(@Param("now") LocalDateTime now);
}


