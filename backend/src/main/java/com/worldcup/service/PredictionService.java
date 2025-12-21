package com.worldcup.service;

import com.worldcup.dto.PerformanceHistoryDTO;
import com.worldcup.dto.PredictionStatisticsDTO;
import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.entity.Notification;
import com.worldcup.entity.Prediction;
import com.worldcup.entity.User;
import com.worldcup.exception.InvalidMatchStateException;
import com.worldcup.exception.MatchNotFoundException;
import com.worldcup.exception.MatchResultNotAvailableException;
import com.worldcup.exception.PredictionLockedException;
import com.worldcup.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final MatchService matchService;
    private final PointsCalculationService pointsCalculationService;
    private final Optional<AchievementService> achievementService; // Optional - may not be available during startup
    private final Optional<NotificationService> notificationService; // Optional - may not be available during startup

    public Prediction createOrUpdatePrediction(User user, Long matchId, 
                                              Integer predictedHomeScore, 
                                              Integer predictedAwayScore) {
        Match match = matchService.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));

        // Check if match is still open for predictions
        // Predictions are allowed only when match status is SCHEDULED
        // Once status changes to LIVE or FINISHED, predictions are locked
        if (match.getStatus() != com.worldcup.entity.MatchStatus.SCHEDULED) {
            throw new PredictionLockedException(match.getStatus());
        }

        Optional<Prediction> existingPrediction = predictionRepository.findByUserAndMatch(user, match);

        Prediction prediction;
        if (existingPrediction.isPresent()) {
            prediction = existingPrediction.get();
            prediction.setPredictedHomeScore(predictedHomeScore);
            prediction.setPredictedAwayScore(predictedAwayScore);
        } else {
            prediction = new Prediction();
            prediction.setUser(user);
            prediction.setMatch(match);
            prediction.setPredictedHomeScore(predictedHomeScore);
            prediction.setPredictedAwayScore(predictedAwayScore);
        }

        return predictionRepository.save(prediction);
    }

    public Optional<Prediction> findByUserAndMatch(User user, Long matchId) {
        Match match = matchService.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));
        return predictionRepository.findByUserAndMatch(user, match);
    }

    public List<Prediction> findByUser(User user) {
        return predictionRepository.findByUser(user);
    }

    public List<Prediction> findByMatch(Long matchId) {
        Match match = matchService.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));
        return predictionRepository.findByMatch(match);
    }

    public Integer calculateTotalPoints(User user) {
        return predictionRepository.calculateTotalPointsByUser(user);
    }

    public void calculatePointsForMatch(Long matchId) {
        Match match = matchService.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));

        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            throw new MatchResultNotAvailableException(matchId);
        }

        // Only calculate points for FINISHED matches
        if (match.getStatus() != MatchStatus.FINISHED) {
            throw new InvalidMatchStateException(match.getStatus(), "calculate points");
        }

        List<Prediction> predictions = predictionRepository.findByMatch(match);

        for (Prediction prediction : predictions) {
            // Skip predictions with null predicted scores
            if (prediction.getPredictedHomeScore() == null || prediction.getPredictedAwayScore() == null) {
                continue;
            }
            
            // Only recalculate if points are null OR if we need to verify correctness
            // This prevents overwriting correct points if called multiple times
            // However, always recalculate to ensure points match current match scores
            // (in case match scores were corrected after initial calculation)
            try {
                int calculatedPoints = pointsCalculationService.calculatePoints(
                    prediction.getPredictedHomeScore(),
                    prediction.getPredictedAwayScore(),
                    match.getHomeScore(),
                    match.getAwayScore()
                );
                
                // Only update if points are null or different (to avoid unnecessary writes)
                // This handles cases where scores were corrected after initial calculation
                Integer existingPoints = prediction.getPoints();
                if (existingPoints == null || !existingPoints.equals(calculatedPoints)) {
                    prediction.setPoints(calculatedPoints);
                    predictionRepository.save(prediction);
                    
                    // Only send notification if this is a new calculation (points were null)
                    // or if points increased (score correction that benefits the user)
                    boolean shouldNotify = existingPoints == null || calculatedPoints > (existingPoints != null ? existingPoints : 0);
                    
                    if (shouldNotify) {
                        notificationService.ifPresent(service -> {
                            try {
                                String message = String.format("%s %d - %d %s. You earned %d point%s!",
                                    match.getHomeTeam(),
                                    match.getHomeScore(),
                                    match.getAwayScore(),
                                    match.getAwayTeam(),
                                    calculatedPoints,
                                    calculatedPoints != 1 ? "s" : ""
                                );
                                
                                service.sendNotification(
                                    prediction.getUser(),
                                    Notification.NotificationType.MATCH_RESULT,
                                    "Match Result",
                                    message,
                                    "⚽",
                                    "/matches?tab=results"
                                );
                            } catch (Exception e) {
                                log.error("Error sending match result notification for prediction {}: {}", 
                                        prediction.getId(), e.getMessage());
                            }
                        });
                    }
                    
                    // Check achievements after points are calculated/updated
                    achievementService.ifPresent(service -> {
                        try {
                            service.checkAchievementsAfterMatchResult(prediction.getUser(), prediction);
                        } catch (Exception e) {
                            log.error("Error checking achievements for prediction {}: {}", prediction.getId(), e.getMessage());
                        }
                    });
                } else {
                    // Points are already correct, skip notification and achievement check
                    log.debug("Points for prediction {} already correct ({}), skipping update", 
                            prediction.getId(), calculatedPoints);
                }
            } catch (Exception e) {
                // Log error but continue processing other predictions
                log.error("Error calculating points for prediction {}: {}", prediction.getId(), e.getMessage());
            }
        }
    }


    public PredictionStatisticsDTO getPredictionStatistics(User user) {
        // Use JOIN FETCH to ensure match data is loaded for filtering
        List<Prediction> predictions;
        try {
            predictions = predictionRepository.findByUserWithMatch(user);
        } catch (Exception e) {
            // Fallback to regular query if JOIN FETCH fails (e.g., data inconsistencies)
            log.warn("JOIN FETCH query failed for user {}, falling back to regular query: {}", user.getId(), e.getMessage());
            predictions = predictionRepository.findByUser(user);
        }
        
        // Filter predictions for matches that have scores
        // CRITICAL: Only include FINISHED or LIVE matches - never SCHEDULED
        // Calculate points on the fly if missing
        List<Prediction> finishedPredictions = predictions.stream()
            .filter(p -> {
                try {
                    Match match = p.getMatch();
                    if (match == null) return false;
                    
                    // Only show predictions for LIVE or FINISHED matches
                    MatchStatus status = match.getStatus();
                    if (status != MatchStatus.FINISHED && status != MatchStatus.LIVE) {
                        return false; // Don't include SCHEDULED or CANCELLED matches
                    }
                    
                    // Must have scores
                    return match.getHomeScore() != null && match.getAwayScore() != null;
                } catch (Exception e) {
                    // Skip predictions with issues (e.g., lazy loading problems)
                    log.warn("Error processing prediction {} in statistics: {}", p.getId(), e.getMessage());
                    return false;
                }
            })
            .map(p -> {
                try {
                    Match match = p.getMatch();
                    MatchStatus status = match != null ? match.getStatus() : null;
                    
                    // Only calculate and save points for FINISHED matches
                    // For LIVE matches, calculate on-the-fly for display only (don't save)
                    if (p.getPoints() == null && match != null && match.getHomeScore() != null && match.getAwayScore() != null &&
                        p.getPredictedHomeScore() != null && p.getPredictedAwayScore() != null) {
                        int points = pointsCalculationService.calculatePoints(
                            p.getPredictedHomeScore(),
                            p.getPredictedAwayScore(),
                            match.getHomeScore(),
                            match.getAwayScore()
                        );
                        
                        // Only save points for FINISHED matches
                        if (status == MatchStatus.FINISHED) {
                            p.setPoints(points);
                            predictionRepository.save(p);
                        } else {
                            // For LIVE matches, set points temporarily for display (won't be saved)
                            p.setPoints(points);
                        }
                    }
                    return p;
                } catch (Exception e) {
                    log.warn("Error calculating points for prediction {}: {}", p.getId(), e.getMessage());
                    return p; // Return prediction anyway, points will remain null
                }
            })
            .collect(Collectors.toList());
        
        int totalPredictions = finishedPredictions.size();
        int exactScores = 0;
        int correctWinners = 0;
        int wrongPredictions = 0;
        int totalPoints = 0;
        
        for (Prediction pred : finishedPredictions) {
            Integer points = pred.getPoints();
            if (points != null) {
                totalPoints += points;
                if (points == PointsCalculationService.EXACT_SCORE_POINTS) {
                    exactScores++;
                } else if (points == PointsCalculationService.CORRECT_WINNER_POINTS) {
                    correctWinners++;
                } else {
                    wrongPredictions++;
                }
            }
        }
        
        // Calculate accuracy: (exact + correct winner) / total * 100
        double accuracyPercentage = totalPredictions > 0 
            ? ((double)(exactScores + correctWinners) / totalPredictions) * 100.0
            : 0.0;
        
        return new PredictionStatisticsDTO(
            totalPredictions,
            exactScores,
            correctWinners,
            wrongPredictions,
            Math.round(accuracyPercentage * 100.0) / 100.0, // Round to 2 decimal places
            totalPoints
        );
    }

    public List<PerformanceHistoryDTO> getPerformanceHistory(User user) {
        // Use JOIN FETCH to ensure match data is loaded
        List<Prediction> predictions;
        try {
            predictions = predictionRepository.findByUserWithMatch(user);
        } catch (Exception e) {
            // Fallback to regular query if JOIN FETCH fails (e.g., data inconsistencies)
            log.warn("JOIN FETCH query failed for user {}, falling back to regular query: {}", user.getId(), e.getMessage());
            predictions = predictionRepository.findByUser(user);
        }
        
        // Filter predictions for matches that have scores
        // CRITICAL: Only include FINISHED or LIVE matches - never SCHEDULED
        // Calculate points on the fly if missing
        List<Prediction> finishedPredictions = predictions.stream()
            .filter(p -> {
                try {
                    Match match = p.getMatch();
                    if (match == null) return false;
                    
                    // Only show predictions for LIVE or FINISHED matches
                    MatchStatus status = match.getStatus();
                    if (status != MatchStatus.FINISHED && status != MatchStatus.LIVE) {
                        return false; // Don't include SCHEDULED or CANCELLED matches
                    }
                    
                    // Must have scores
                    return match.getHomeScore() != null && match.getAwayScore() != null;
                } catch (Exception e) {
                    // Skip predictions with issues (e.g., lazy loading problems)
                    log.warn("Error processing prediction {} in performance history: {}", p.getId(), e.getMessage());
                    return false;
                }
            })
            .map(p -> {
                try {
                    Match match = p.getMatch();
                    MatchStatus status = match != null ? match.getStatus() : null;
                    
                    // Only calculate and save points for FINISHED matches
                    // For LIVE matches, calculate on-the-fly for display only (don't save)
                    if (p.getPoints() == null && match != null && match.getHomeScore() != null && match.getAwayScore() != null &&
                        p.getPredictedHomeScore() != null && p.getPredictedAwayScore() != null) {
                        int points = pointsCalculationService.calculatePoints(
                            p.getPredictedHomeScore(),
                            p.getPredictedAwayScore(),
                            match.getHomeScore(),
                            match.getAwayScore()
                        );
                        
                        // Only save points for FINISHED matches
                        if (status == MatchStatus.FINISHED) {
                            p.setPoints(points);
                            predictionRepository.save(p);
                        } else {
                            // For LIVE matches, set points temporarily for display (won't be saved)
                            p.setPoints(points);
                        }
                    }
                    return p;
                } catch (Exception e) {
                    log.warn("Error calculating points for prediction {}: {}", p.getId(), e.getMessage());
                    return p; // Return prediction anyway, points will remain null
                }
            })
            .sorted((p1, p2) -> {
                try {
                    return p1.getMatch().getMatchDate().compareTo(p2.getMatch().getMatchDate());
                } catch (Exception e) {
                    log.warn("Error sorting predictions: {}", e.getMessage());
                    return 0;
                }
            })
            .collect(Collectors.toList());
        
        List<PerformanceHistoryDTO> history = new ArrayList<>();
        int cumulativePoints = 0;
        
        for (Prediction pred : finishedPredictions) {
            Match match = pred.getMatch();
            Integer points = pred.getPoints();
            cumulativePoints += (points != null ? points : 0);
            
            String resultType;
            if (points == null || points == PointsCalculationService.WRONG_PREDICTION_POINTS) {
                resultType = "WRONG";
            } else if (points == PointsCalculationService.EXACT_SCORE_POINTS) {
                resultType = "EXACT";
            } else {
                resultType = "CORRECT_WINNER";
            }
            
            history.add(new PerformanceHistoryDTO(
                match.getId(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getMatchDate(),
                pred.getPredictedHomeScore(),
                pred.getPredictedAwayScore(),
                match.getHomeScore(),
                match.getAwayScore(),
                points,
                resultType,
                cumulativePoints
            ));
        }
        
        return history;
    }
}

