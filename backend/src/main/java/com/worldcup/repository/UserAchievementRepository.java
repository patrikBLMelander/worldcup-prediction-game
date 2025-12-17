package com.worldcup.repository;

import com.worldcup.entity.Achievement;
import com.worldcup.entity.User;
import com.worldcup.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserOrderByEarnedAtDesc(User user);
    Optional<UserAchievement> findByUserAndAchievement(User user, Achievement achievement);
    boolean existsByUserAndAchievement(User user, Achievement achievement);
    
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user = :user AND ua.achievement.code = :achievementCode")
    Optional<UserAchievement> findByUserAndAchievementCode(@Param("user") User user, @Param("achievementCode") String achievementCode);
}

