package com.worldcup.repository;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.entity.Prediction;
import com.worldcup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    Optional<Prediction> findByUserAndMatch(User user, Match match);
    List<Prediction> findByUser(User user);
    
    @Query("SELECT p FROM Prediction p JOIN FETCH p.match WHERE p.user = :user")
    List<Prediction> findByUserWithMatch(@Param("user") User user);
    
    /**
     * Optimized query to get finished predictions for a user, sorted by match date.
     * Used for achievement checking to avoid loading all predictions.
     */
    @Query("SELECT p FROM Prediction p JOIN FETCH p.match " +
           "WHERE p.user = :user AND p.match.status = :status " +
           "ORDER BY p.match.matchDate ASC")
    List<Prediction> findByUserAndMatchStatus(
        @Param("user") User user, 
        @Param("status") MatchStatus status
    );
    
    List<Prediction> findByMatch(Match match);
    
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM Prediction p WHERE p.user = :user")
    Integer calculateTotalPointsByUser(@Param("user") User user);
    
    @Query("SELECT p.user.id, COALESCE(SUM(p.points), 0) as totalPoints " +
           "FROM Prediction p " +
           "GROUP BY p.user.id " +
           "ORDER BY totalPoints DESC")
    List<Object[]> findLeaderboard();

    // Deletes predictions tied to any non-World-Cup match. Mirrors the
    // allow-list in MatchRepository.deleteNonWorldCupMatches(); must run
    // first to satisfy the predictions.match_id foreign key.
    @Modifying
    @Query(value = "DELETE FROM predictions WHERE match_id IN (" +
            "  SELECT id FROM matches WHERE NOT (" +
            "    external_api_id IS NOT NULL AND external_api_id <> '' " +
            "    AND (match_group LIKE 'Group %' " +
            "         OR match_group IN ('Round of 32','Round of 16','Quarter-Final'," +
            "                            'Semi-Final','Third-Place Play-off','Final'))" +
            "  )" +
            ")",
            nativeQuery = true)
    int deletePredictionsForNonWorldCupMatches();
}


