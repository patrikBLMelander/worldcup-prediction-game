package com.worldcup.repository;

import com.worldcup.entity.Prediction;
import com.worldcup.entity.User;
import com.worldcup.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
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
    
    List<Prediction> findByMatch(Match match);
    
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM Prediction p WHERE p.user = :user")
    Integer calculateTotalPointsByUser(@Param("user") User user);
    
    @Query("SELECT p.user.id, COALESCE(SUM(p.points), 0) as totalPoints " +
           "FROM Prediction p " +
           "GROUP BY p.user.id " +
           "ORDER BY totalPoints DESC")
    List<Object[]> findLeaderboard();
}


