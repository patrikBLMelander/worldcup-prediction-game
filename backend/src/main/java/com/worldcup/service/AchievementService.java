package com.worldcup.service;

import com.worldcup.dto.LeaderboardEntryDTO;
import com.worldcup.entity.*;
import com.worldcup.repository.AchievementRepository;
import com.worldcup.repository.PredictionRepository;
import com.worldcup.repository.UserAchievementRepository;
import com.worldcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;

    /**
     * Check and award achievements after a prediction is made
     */
    public void checkAchievementsAfterPrediction(User user) {
        List<Prediction> userPredictions = predictionRepository.findByUser(user);
        
        // First Prediction
        if (userPredictions.size() == 1) {
            awardAchievement(user, "FIRST_PREDICTION");
        }
        
        // Milestone achievements
        int predictionCount = userPredictions.size();
        if (predictionCount >= 10) awardAchievement(user, "MILESTONE_10");
        if (predictionCount >= 25) awardAchievement(user, "MILESTONE_25");
        if (predictionCount >= 50) awardAchievement(user, "MILESTONE_50");
        if (predictionCount >= 100) awardAchievement(user, "MILESTONE_100");
        if (predictionCount >= 250) awardAchievement(user, "MILESTONE_250");
    }

    /**
     * Check and award achievements after points are calculated for a match
     */
    public void checkAchievementsAfterMatchResult(User user, Prediction prediction) {
        if (prediction.getPoints() == null) return;
        
        Match match = prediction.getMatch();
        if (match == null || match.getStatus() != MatchStatus.FINISHED) return;
        
        // Load finished predictions once for all streak checks (performance optimization)
        List<Prediction> finishedPredictions = getFinishedPredictionsForUser(user);
        
        // Exact Score (3 points)
        if (prediction.getPoints() == 3) {
            awardAchievement(user, "EXACT_SCORE");
            
            // Check for multiple exact scores
            checkExactScoreStreaks(user, finishedPredictions);
        }
        
        // Correct Winner (1 point) or Exact Score (3 points)
        if (prediction.getPoints() > 0) {
            checkCorrectWinnerStreaks(user, finishedPredictions);
        }
        
        // Check for perfect week (7 consecutive correct predictions)
        checkPerfectWeek(user, finishedPredictions);
        
        // Check for comeback predictions (predicting underdog correctly)
        checkComebackPrediction(user, prediction, match, finishedPredictions);
    }
    
    /**
     * Helper method to get finished predictions for a user (optimized query)
     */
    private List<Prediction> getFinishedPredictionsForUser(User user) {
        return predictionRepository.findByUserAndMatchStatus(user, MatchStatus.FINISHED);
    }

    /**
     * Check leaderboard position achievements for a finished league
     * Only awards achievements when a league has finished (endDate has passed)
     */
    public void checkLeagueLeaderboardAchievements(League league, List<LeaderboardEntryDTO> leaderboard) {
        // Only check if league has finished
        if (league.getEndDate().isAfter(LocalDateTime.now())) {
            return; // League hasn't finished yet
        }
        
        // Award achievements based on position in this league
        for (int i = 0; i < leaderboard.size(); i++) {
            LeaderboardEntryDTO entry = leaderboard.get(i);
            int position = i + 1;
            
            // Get user from entry
            User user = userRepository.findById(entry.getUserId()).orElse(null);
            if (user == null) continue;
            
            // Award achievements based on position
            if (position == 1) awardAchievement(user, "LEADERBOARD_1");
            if (position <= 3) awardAchievement(user, "LEADERBOARD_TOP_3");
            if (position <= 10) awardAchievement(user, "LEADERBOARD_TOP_10");
            if (position <= 50) awardAchievement(user, "LEADERBOARD_TOP_50");
        }
    }

    /**
     * Check for exact score streaks
     * Streaks must be consecutive finished matches (by date) where user got exact scores
     */
    private void checkExactScoreStreaks(User user, List<Prediction> finishedPredictions) {
        // Early exit if user already has highest streak achievement
        if (userAchievementRepository.findByUserAndAchievementCode(user, "EXACT_STREAK_5").isPresent()) {
            return;
        }
        
        // Find the longest consecutive streak of exact scores (3 points)
        int currentConsecutiveCount = 0;
        
        for (Prediction p : finishedPredictions) {
            if (p.getPoints() != null && p.getPoints() == 3) {
                // Exact score - continue streak
                currentConsecutiveCount++;
                
                // Award achievements when reaching milestones
                if (currentConsecutiveCount == 2) awardAchievement(user, "EXACT_STREAK_2");
                if (currentConsecutiveCount == 3) awardAchievement(user, "EXACT_STREAK_3");
                if (currentConsecutiveCount == 5) awardAchievement(user, "EXACT_STREAK_5");
            } else {
                // Not an exact score - reset streak
                currentConsecutiveCount = 0;
            }
        }
    }

    /**
     * Check for correct winner streaks
     * Streaks must be consecutive finished matches (by date) where user got all correct
     */
    private void checkCorrectWinnerStreaks(User user, List<Prediction> finishedPredictions) {
        // Early exit if user already has highest streak achievement
        if (userAchievementRepository.findByUserAndAchievementCode(user, "STREAK_20").isPresent()) {
            return;
        }
        
        // Find the longest consecutive streak of correct predictions
        int currentConsecutiveCount = 0;
        
        for (Prediction p : finishedPredictions) {
            if (p.getPoints() != null && p.getPoints() > 0) {
                // Correct prediction - continue streak
                currentConsecutiveCount++;
                
                // Award achievements when reaching milestones
                if (currentConsecutiveCount == 5) awardAchievement(user, "STREAK_5");
                if (currentConsecutiveCount == 10) awardAchievement(user, "STREAK_10");
                if (currentConsecutiveCount == 15) awardAchievement(user, "STREAK_15");
                if (currentConsecutiveCount == 20) awardAchievement(user, "STREAK_20");
            } else {
                // Incorrect prediction - reset streak
                currentConsecutiveCount = 0;
            }
        }
    }

    /**
     * Check for perfect week (7 consecutive correct predictions within a week)
     * Only awards once per user
     */
    private void checkPerfectWeek(User user, List<Prediction> finishedPredictions) {
        // Early exit if user already has this achievement
        if (userAchievementRepository.findByUserAndAchievementCode(user, "PERFECT_WEEK").isPresent()) {
            return;
        }
        
        // Check for 7 consecutive correct predictions within 7 days
        for (int i = 0; i <= finishedPredictions.size() - 7; i++) {
            // Check if next 7 predictions are all correct
            boolean allCorrect = true;
            for (int j = i; j < i + 7; j++) {
                Prediction p = finishedPredictions.get(j);
                if (p.getPoints() == null || p.getPoints() == 0) {
                    allCorrect = false;
                    break;
                }
            }
            
            if (!allCorrect) continue;
            
            // Check if within 7 days
            LocalDateTime firstDate = finishedPredictions.get(i).getMatch().getMatchDate();
            LocalDateTime lastDate = finishedPredictions.get(i + 6).getMatch().getMatchDate();
            
            if (ChronoUnit.DAYS.between(firstDate, lastDate) <= 7) {
                awardAchievement(user, "PERFECT_WEEK");
                break; // Only award once
            }
        }
    }

    /**
     * Check for comeback prediction
     * Awards if user had 3 consecutive 0-point predictions, then gets a 3-point (exact score) prediction
     * This represents a comeback from a losing streak to a perfect prediction
     */
    private void checkComebackPrediction(User user, Prediction prediction, Match match, List<Prediction> finishedPredictions) {
        // Only check if this prediction is worth 3 points (exact score)
        if (prediction.getPoints() == null || prediction.getPoints() != 3) return;
        if (match.getHomeScore() == null || match.getAwayScore() == null) return;
        
        // Early exit if user already has this achievement
        if (userAchievementRepository.findByUserAndAchievementCode(user, "COMEBACK_KING").isPresent()) {
            return;
        }
        
        // Find the current prediction's position
        int currentIndex = -1;
        for (int i = 0; i < finishedPredictions.size(); i++) {
            if (finishedPredictions.get(i).getId().equals(prediction.getId())) {
                currentIndex = i;
                break;
            }
        }
        
        // Need at least 4 predictions total (3 wrong + 1 correct)
        if (currentIndex < 3) return;
        
        // Check if the 3 predictions before this one were all 0 points
        boolean hasThreeZeroPointers = true;
        for (int i = currentIndex - 3; i < currentIndex; i++) {
            Prediction p = finishedPredictions.get(i);
            if (p.getPoints() == null || p.getPoints() != 0) {
                hasThreeZeroPointers = false;
                break;
            }
        }
        
        // If we had 3 consecutive 0-pointers before this 3-pointer, it's a comeback!
        if (hasThreeZeroPointers) {
            awardAchievement(user, "COMEBACK_KING");
        }
    }

    /**
     * Award an achievement to a user if they don't already have it
     */
    public void awardAchievement(User user, String achievementCode) {
        if (user == null || achievementCode == null || achievementCode.trim().isEmpty()) {
            return;
        }
        
        try {
            // Check if user already has this achievement
            Achievement achievement = achievementRepository.findByCode(achievementCode)
                .orElse(null);
            
            if (achievement == null || !achievement.getActive()) {
                log.debug("Achievement {} not found or inactive", achievementCode);
                return;
            }
            
            boolean alreadyHas = userAchievementRepository.existsByUserAndAchievement(user, achievement);
            if (alreadyHas) {
                return; // User already has this achievement
            }
            
            // Award the achievement
            UserAchievement userAchievement = new UserAchievement();
            userAchievement.setUser(user);
            userAchievement.setAchievement(achievement);
            
            try {
                userAchievementRepository.save(userAchievement);
                log.info("Achievement {} awarded to user {}", achievementCode, user.getId());
            } catch (DataIntegrityViolationException e) {
                // Another thread/request already awarded this achievement (race condition)
                // This is safe to ignore - the unique constraint prevented the duplicate
                log.debug("Achievement {} already awarded to user {} (race condition handled)", 
                         achievementCode, user.getId());
            }
        } catch (Exception e) {
            log.error("Error awarding achievement {} to user {}: {}", 
                     achievementCode, user.getId(), e.getMessage());
        }
    }

    /**
     * Get all achievements for a user
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getUserAchievements(User user) {
        return userAchievementRepository.findByUserOrderByEarnedAtDesc(user);
    }

    /**
     * Get all available achievements
     */
    @Transactional(readOnly = true)
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findByActiveTrueOrderByCategoryAscRarityDesc();
    }

    /**
     * Initialize default achievements in the database
     * This should be called on application startup or via admin endpoint
     */
    public void initializeDefaultAchievements() {
        // Milestone Achievements
        createAchievementIfNotExists("FIRST_PREDICTION", "First Prediction", 
            "Make your first prediction", "🎯", "MILESTONE", 1);
        createAchievementIfNotExists("MILESTONE_10", "Decade", 
            "Make 10 predictions", "🔟", "MILESTONE", 1);
        createAchievementIfNotExists("MILESTONE_25", "Quarter Century", 
            "Make 25 predictions", "📊", "MILESTONE", 2);
        createAchievementIfNotExists("MILESTONE_50", "Half Century", 
            "Make 50 predictions", "🏅", "MILESTONE", 2);
        createAchievementIfNotExists("MILESTONE_100", "Century", 
            "Make 100 predictions", "💯", "MILESTONE", 3);
        createAchievementIfNotExists("MILESTONE_250", "Legend", 
            "Make 250 predictions", "👑", "MILESTONE", 4);

        // Performance Achievements
        createAchievementIfNotExists("EXACT_SCORE", "Perfect Score", 
            "Predict the exact score of a match", "⭐", "PERFORMANCE", 2);
        createAchievementIfNotExists("EXACT_STREAK_2", "Double Perfect", 
            "Get 2 exact scores in a row", "⭐⭐", "PERFORMANCE", 3);
        createAchievementIfNotExists("EXACT_STREAK_3", "Triple Perfect", 
            "Get 3 exact scores in a row", "⭐⭐⭐", "PERFORMANCE", 4);
        createAchievementIfNotExists("EXACT_STREAK_5", "Perfect Storm", 
            "Get 5 exact scores in a row", "⚡", "PERFORMANCE", 5);

        // Streak Achievements
        createAchievementIfNotExists("STREAK_5", "On Fire", 
            "Get 5 correct predictions in a row", "🔥", "STREAK", 2);
        createAchievementIfNotExists("STREAK_10", "Unstoppable", 
            "Get 10 correct predictions in a row", "🚀", "STREAK", 3);
        createAchievementIfNotExists("STREAK_15", "Unbeatable", 
            "Get 15 correct predictions in a row", "💪", "STREAK", 4);
        createAchievementIfNotExists("STREAK_20", "Legendary Streak", 
            "Get 20 correct predictions in a row", "👑", "STREAK", 5);
        createAchievementIfNotExists("PERFECT_WEEK", "Perfect Week", 
            "Get 7 correct predictions in one week", "📅", "STREAK", 3);

        // Leaderboard Achievements
        createAchievementIfNotExists("LEADERBOARD_TOP_50", "Top 50", 
            "Reach top 50 on the leaderboard", "🏆", "LEADERBOARD", 2);
        createAchievementIfNotExists("LEADERBOARD_TOP_10", "Top 10", 
            "Reach top 10 on the leaderboard", "🥇", "LEADERBOARD", 3);
        createAchievementIfNotExists("LEADERBOARD_TOP_3", "Top 3", 
            "Reach top 3 on the leaderboard", "🥈", "LEADERBOARD", 4);
        createAchievementIfNotExists("LEADERBOARD_1", "Champion", 
            "Reach #1 on the leaderboard", "👑", "LEADERBOARD", 5);

        // Special Achievements
        createAchievementIfNotExists("COMEBACK_KING", "Comeback King", 
            "Get an exact score after 3 consecutive wrong predictions", "🎲", "SPECIAL", 3);
    }

    private void createAchievementIfNotExists(String code, String name, String description, 
                                             String icon, String category, Integer rarity) {
        if (!achievementRepository.findByCode(code).isPresent()) {
            Achievement achievement = new Achievement();
            achievement.setCode(code);
            achievement.setName(name);
            achievement.setDescription(description);
            achievement.setIcon(icon);
            achievement.setCategory(category);
            achievement.setRarity(rarity);
            achievement.setActive(true);
            achievementRepository.save(achievement);
            log.info("Created achievement: {}", code);
        }
    }
}

