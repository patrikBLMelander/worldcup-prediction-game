package com.worldcup.controller;

import com.worldcup.dto.AchievementDTO;
import com.worldcup.dto.ChangePasswordRequest;
import com.worldcup.dto.FinishedPredictionDTO;
import com.worldcup.dto.LeaderboardEntryDTO;
import com.worldcup.dto.PerformanceHistoryDTO;
import com.worldcup.dto.PredictionStatisticsDTO;
import com.worldcup.dto.PublicProfileDTO;
import com.worldcup.dto.UpdateScreenNameRequest;
import com.worldcup.dto.UserProfileDTO;
import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.entity.Prediction;
import com.worldcup.entity.User;
import com.worldcup.entity.Achievement;
import com.worldcup.entity.UserAchievement;
import com.worldcup.exception.UserNotFoundException;
import com.worldcup.repository.AchievementRepository;
import com.worldcup.repository.MatchRepository;
import com.worldcup.repository.PredictionRepository;
import com.worldcup.repository.UserAchievementRepository;
import com.worldcup.repository.UserRepository;
import com.worldcup.security.CurrentUser;
import com.worldcup.service.PointsCalculationService;
import com.worldcup.service.PredictionService;
import com.worldcup.service.UserService;

import static com.worldcup.service.PointsCalculationService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final CurrentUser currentUser;
    private final PredictionService predictionService;
    private final PointsCalculationService pointsCalculationService;
    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;
    private final UserService userService;
    private final MatchRepository matchRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        User user = currentUser.getCurrentUserOrThrow();
        Integer totalPoints = predictionService.calculateTotalPoints(user);
        long predictionCount = predictionRepository.findByUser(user).size();

        UserProfileDTO profile = new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getScreenName(),
                user.getRole().name(),
                totalPoints,
                (int) predictionCount,
                user.getCreatedAt()
        );

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard() {
        List<Object[]> leaderboardData = predictionRepository.findLeaderboard();
        
        List<LeaderboardEntryDTO> leaderboard = leaderboardData.stream()
                .map(row -> {
                    Long userId = ((Number) row[0]).longValue();
                    Integer totalPoints = ((Number) row[1]).intValue();
                    
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UserNotFoundException(userId));
                    
                    long predictionCount = predictionRepository.findByUser(user).size();
                    
                    return new LeaderboardEntryDTO(
                            userId,
                            user.getEmail(),
                            user.getScreenName(),
                            totalPoints,
                            (int) predictionCount,
                            null, // prizeAmount - not applicable for global leaderboard
                            null  // rank - will be assigned by sorting
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(leaderboard);
    }

    @PutMapping("/me/screen-name")
    public ResponseEntity<UserProfileDTO> updateScreenName(@Valid @RequestBody UpdateScreenNameRequest request) {
        User user = currentUser.getCurrentUserOrThrow();
        user.setScreenName(request.getScreenName());
        userRepository.save(user);

        Integer totalPoints = predictionService.calculateTotalPoints(user);
        long predictionCount = predictionRepository.findByUser(user).size();

        UserProfileDTO profile = new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getScreenName(),
                user.getRole().name(),
                totalPoints,
                (int) predictionCount,
                user.getCreatedAt()
        );

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUser.getCurrentUserOrThrow();
        
        try {
            userService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok().body(java.util.Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me/prediction-statistics")
    public ResponseEntity<PredictionStatisticsDTO> getPredictionStatistics() {
        User user = currentUser.getCurrentUserOrThrow();
        PredictionStatisticsDTO statistics = predictionService.getPredictionStatistics(user);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/me/performance-history")
    public ResponseEntity<List<PerformanceHistoryDTO>> getPerformanceHistory() {
        User user = currentUser.getCurrentUserOrThrow();
        List<PerformanceHistoryDTO> history = predictionService.getPerformanceHistory(user);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/me/upcoming-matches-without-prediction")
    public ResponseEntity<List<com.worldcup.dto.MatchDTO>> getUpcomingMatchesWithoutPrediction() {
        User user = currentUser.getCurrentUserOrThrow();
        
        // Get all scheduled matches in the future
        List<Match> upcomingMatches = matchRepository.findByStatusAndMatchDateAfter(
            MatchStatus.SCHEDULED, 
            LocalDateTime.now()
        );
        
        // Get all match IDs where user has predictions
        Set<Long> matchIdsWithPredictions = predictionRepository.findByUser(user).stream()
            .map(p -> p.getMatch().getId())
            .collect(Collectors.toSet());
        
        // Filter out matches where user already has a prediction
        List<Match> matchesWithoutPrediction = upcomingMatches.stream()
            .filter(m -> !matchIdsWithPredictions.contains(m.getId()))
            .sorted((m1, m2) -> m1.getMatchDate().compareTo(m2.getMatchDate()))
            .limit(10) // Limit to next 10 matches
            .collect(Collectors.toList());
        
        // Convert to DTOs
        List<com.worldcup.dto.MatchDTO> matchDTOs = matchesWithoutPrediction.stream()
            .map(match -> new com.worldcup.dto.MatchDTO(
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
                match.getAwayScore()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(matchDTOs);
    }

    @GetMapping("/{userId}/public-profile")
    public ResponseEntity<PublicProfileDTO> getPublicProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        Integer totalPoints = predictionService.calculateTotalPoints(user);
        long predictionCount = predictionRepository.findByUser(user).size();
        
        // Get statistics for finished matches only
        PredictionStatisticsDTO statistics = predictionService.getPredictionStatistics(user);
        
        // Get finished predictions - ONLY show predictions for matches that are LIVE or FINISHED
        // Never show predictions for SCHEDULED matches (even if they have scores somehow)
        // Use JOIN FETCH to ensure match data is loaded
        List<Prediction> allPredictions;
        try {
            allPredictions = predictionRepository.findByUserWithMatch(user);
        } catch (Exception e) {
            // Fallback to regular query if JOIN FETCH fails (e.g., data inconsistencies)
            log.warn("JOIN FETCH query failed for user {}, falling back to regular query: {}", user.getId(), e.getMessage());
            allPredictions = predictionRepository.findByUser(user);
        }
        
        List<FinishedPredictionDTO> finishedPredictions = allPredictions.stream()
                .filter(p -> {
                    try {
                        Match match = p.getMatch();
                        if (match == null) return false;
                        
                        // CRITICAL: Only show predictions for LIVE or FINISHED matches
                        // Never show predictions for SCHEDULED matches (game hasn't started yet)
                        MatchStatus status = match.getStatus();
                        if (status != MatchStatus.FINISHED && status != MatchStatus.LIVE) {
                            return false; // Don't show predictions for SCHEDULED or CANCELLED matches
                        }
                        
                        // Must have scores to show the prediction
                        boolean hasScores = match.getHomeScore() != null && match.getAwayScore() != null;
                        return hasScores;
                    } catch (Exception e) {
                        // Skip predictions with issues (e.g., lazy loading problems)
                        log.warn("Error processing prediction {}: {}", p.getId(), e.getMessage());
                        return false;
                    }
                })
                .sorted((p1, p2) -> p2.getMatch().getMatchDate().compareTo(p1.getMatch().getMatchDate())) // Most recent first
                .map(p -> {
                    Match match = p.getMatch();
                    Integer points = p.getPoints();
                    MatchStatus status = match.getStatus();
                    
                    // Only calculate and save points for FINISHED matches
                    // For LIVE matches, only display points if already calculated (don't calculate new ones as scores can change)
                    if (points == null && status == MatchStatus.FINISHED && match.getHomeScore() != null && match.getAwayScore() != null) {
                        points = pointsCalculationService.calculatePoints(
                            p.getPredictedHomeScore(),
                            p.getPredictedAwayScore(),
                            match.getHomeScore(),
                            match.getAwayScore()
                        );
                        // Save the calculated points
                        p.setPoints(points);
                        predictionRepository.save(p);
                    }
                    
                    // For LIVE matches, calculate points on-the-fly for display only (don't save)
                    if (points == null && status == MatchStatus.LIVE && match.getHomeScore() != null && match.getAwayScore() != null) {
                        points = pointsCalculationService.calculatePoints(
                            p.getPredictedHomeScore(),
                            p.getPredictedAwayScore(),
                            match.getHomeScore(),
                            match.getAwayScore()
                        );
                        // Don't save - match is still LIVE, scores can change
                    }
                    
                    String resultType;
                    if (points == null || points == WRONG_PREDICTION_POINTS) {
                        resultType = "WRONG";
                    } else if (points == EXACT_SCORE_POINTS) {
                        resultType = "EXACT";
                    } else {
                        resultType = "CORRECT_WINNER";
                    }
                    
                    return new FinishedPredictionDTO(
                            match.getId(),
                            match.getHomeTeam(),
                            match.getHomeTeamCrest(),
                            match.getAwayTeam(),
                            match.getAwayTeamCrest(),
                            match.getMatchDate(),
                            match.getVenue(),
                            match.getGroup(),
                            p.getPredictedHomeScore(),
                            p.getPredictedAwayScore(),
                            match.getHomeScore(),
                            match.getAwayScore(),
                            points,
                            resultType,
                            match.getStatus().name()
                    );
                })
                .collect(Collectors.toList());
        
        PublicProfileDTO profile = new PublicProfileDTO(
                user.getId(),
                user.getScreenName(),
                totalPoints,
                (int) predictionCount,
                statistics,
                finishedPredictions
        );
        
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me/achievements")
    public ResponseEntity<List<AchievementDTO>> getMyAchievements() {
        User user = currentUser.getCurrentUserOrThrow();
        
        // Get user's earned achievements
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserOrderByEarnedAtDesc(user);
        
        // Convert to DTOs - only show earned achievements
        List<AchievementDTO> achievementDTOs = userAchievements.stream()
            .map(ua -> {
                Achievement achievement = ua.getAchievement();
                return new AchievementDTO(
                    achievement.getId(),
                    achievement.getCode(),
                    achievement.getName(),
                    achievement.getDescription(),
                    achievement.getIcon(),
                    achievement.getCategory(),
                    achievement.getRarity(),
                    true,
                    ua.getEarnedAt().toString()
                );
            })
            .collect(Collectors.toList());
        
        // Sort by category, then by rarity (higher first)
        achievementDTOs.sort((a1, a2) -> {
            int categoryCompare = a1.getCategory().compareTo(a2.getCategory());
            if (categoryCompare != 0) return categoryCompare;
            return Integer.compare(a2.getRarity(), a1.getRarity()); // Higher rarity first
        });
        
        return ResponseEntity.ok(achievementDTOs);
    }

    @GetMapping("/{userId}/achievements")
    public ResponseEntity<List<AchievementDTO>> getUserAchievements(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Get all available achievements
        List<Achievement> allAchievements = achievementRepository.findByActiveTrueOrderByCategoryAscRarityDesc();
        
        // Get user's earned achievements
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserOrderByEarnedAtDesc(user);
        Set<String> earnedCodes = userAchievements.stream()
            .map(ua -> ua.getAchievement().getCode())
            .collect(Collectors.toSet());
        
        // Create a map of earned achievements by code for quick lookup
        Map<String, UserAchievement> earnedMap = userAchievements.stream()
            .collect(Collectors.toMap(
                ua -> ua.getAchievement().getCode(),
                ua -> ua
            ));
        
        // Convert to DTOs - only show earned achievements for public profile
        List<AchievementDTO> achievementDTOs = allAchievements.stream()
            .filter(achievement -> earnedCodes.contains(achievement.getCode())) // Only show earned
            .map(achievement -> {
                UserAchievement ua = earnedMap.get(achievement.getCode());
                return new AchievementDTO(
                    achievement.getId(),
                    achievement.getCode(),
                    achievement.getName(),
                    achievement.getDescription(),
                    achievement.getIcon(),
                    achievement.getCategory(),
                    achievement.getRarity(),
                    true, // Always true since we filtered
                    ua.getEarnedAt().toString()
                );
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(achievementDTOs);
    }
}

