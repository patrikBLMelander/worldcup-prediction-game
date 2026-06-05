package com.worldcup.service;

import com.worldcup.dto.PerformanceHistoryDTO;
import com.worldcup.dto.PredictionStatisticsDTO;
import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.entity.Notification;
import com.worldcup.entity.Prediction;
import com.worldcup.entity.PredictionOutcome;
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
    private final RelativeScoringService relativeScoringService;
    private final Optional<NotificationService> notificationService; // Optional - may not be available during startup

    public Prediction createOrUpdatePrediction(User user, Long matchId, PredictionOutcome predictedOutcome) {
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
            prediction.setPredictedOutcome(predictedOutcome);
        } else {
            prediction = new Prediction();
            prediction.setUser(user);
            prediction.setMatch(match);
            prediction.setPredictedOutcome(predictedOutcome);
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

    /**
     * Relative points for a single prediction in the GLOBAL pool (all players who
     * predicted this match), or {@code null} if the match isn't scorable yet.
     *
     * <p>Used for on-the-fly display of LIVE matches and as a fallback when stored
     * points are missing. For FINISHED matches the value is stored on the prediction.
     */
    public Integer computeGlobalPoints(Prediction prediction) {
        Match match = prediction.getMatch();
        if (match == null || match.getHomeScore() == null || match.getAwayScore() == null) {
            return null;
        }
        if (prediction.getPredictedOutcome() == null) {
            return null;
        }

        PredictionOutcome actual = relativeScoringService.actualOutcome(
                match.getHomeScore(), match.getAwayScore());

        if (prediction.getPredictedOutcome() != actual) {
            return 0;
        }

        List<Prediction> all = predictionRepository.findByMatch(match);
        int predictorCount = 0;
        int correctCount = 0;
        for (Prediction other : all) {
            if (other.getPredictedOutcome() == null) continue;
            predictorCount++;
            if (other.getPredictedOutcome() == actual) correctCount++;
        }

        int gameValue = relativeScoringService.gameValue(match.getGroup());
        return relativeScoringService.correctPredictionPoints(gameValue, correctCount, predictorCount);
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

        PredictionOutcome actual = relativeScoringService.actualOutcome(
                match.getHomeScore(), match.getAwayScore());

        // Count predictors and how many got the outcome right (global pool).
        // Predictions are locked at kickoff, so these counts are final.
        int predictorCount = 0;
        int correctCount = 0;
        for (Prediction p : predictions) {
            if (p.getPredictedOutcome() == null) continue;
            predictorCount++;
            if (p.getPredictedOutcome() == actual) correctCount++;
        }

        int gameValue = relativeScoringService.gameValue(match.getGroup());
        int correctPoints = relativeScoringService.correctPredictionPoints(
                gameValue, correctCount, predictorCount);

        for (Prediction prediction : predictions) {
            if (prediction.getPredictedOutcome() == null) {
                continue; // legacy row without an outcome; nothing to score
            }

            boolean isCorrect = prediction.getPredictedOutcome() == actual;
            int points = isCorrect ? correctPoints : 0;

            try {
                Integer existingPoints = prediction.getPoints();
                boolean firstCalculation = existingPoints == null;

                if (existingPoints == null || !existingPoints.equals(points)) {
                    prediction.setPoints(points);
                    predictionRepository.save(prediction);
                }

                // Outcome-only notification on the first calculation (no point number,
                // since points differ per league).
                if (firstCalculation) {
                    notificationService.ifPresent(service -> {
                        try {
                            String message = String.format("%s %d - %d %s. %s",
                                match.getHomeTeam(),
                                match.getHomeScore(),
                                match.getAwayScore(),
                                match.getAwayTeam(),
                                isCorrect ? "You called the result! ✅"
                                          : "Your pick didn't come in this time."
                            );

                            service.sendNotification(
                                prediction.getUser(),
                                Notification.NotificationType.MATCH_RESULT,
                                "Match Result",
                                message,
                                isCorrect ? "✅" : "⚽",
                                "/matches?tab=results"
                            );
                        } catch (Exception e) {
                            log.error("Error sending match result notification for prediction {}: {}",
                                    prediction.getId(), e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                // Log error but continue processing other predictions
                log.error("Error calculating points for prediction {}: {}", prediction.getId(), e.getMessage());
            }
        }
    }


    public PredictionStatisticsDTO getPredictionStatistics(User user) {
        List<Prediction> finishedPredictions = scorablePredictionsForUser(user);

        int totalPredictions = 0;
        int correctPredictions = 0;
        int wrongPredictions = 0;
        int totalPoints = 0;

        for (Prediction pred : finishedPredictions) {
            Match match = pred.getMatch();
            PredictionOutcome actual = relativeScoringService.actualOutcome(
                    match.getHomeScore(), match.getAwayScore());
            boolean correct = pred.getPredictedOutcome() == actual;

            totalPredictions++;
            if (correct) {
                correctPredictions++;
            } else {
                wrongPredictions++;
            }

            Integer points = pointsForDisplay(pred);
            if (points != null) {
                totalPoints += points;
            }
        }

        double accuracyPercentage = totalPredictions > 0
            ? ((double) correctPredictions / totalPredictions) * 100.0
            : 0.0;

        return new PredictionStatisticsDTO(
            totalPredictions,
            correctPredictions,
            wrongPredictions,
            Math.round(accuracyPercentage * 100.0) / 100.0, // Round to 2 decimal places
            totalPoints
        );
    }

    public List<PerformanceHistoryDTO> getPerformanceHistory(User user) {
        List<Prediction> finishedPredictions = scorablePredictionsForUser(user).stream()
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
            PredictionOutcome actual = relativeScoringService.actualOutcome(
                    match.getHomeScore(), match.getAwayScore());
            boolean correct = pred.getPredictedOutcome() == actual;

            Integer points = pointsForDisplay(pred);
            cumulativePoints += (points != null ? points : 0);

            history.add(new PerformanceHistoryDTO(
                match.getId(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getMatchDate(),
                pred.getPredictedOutcome(),
                match.getHomeScore(),
                match.getAwayScore(),
                points,
                correct ? "CORRECT" : "WRONG",
                cumulativePoints
            ));
        }

        return history;
    }

    /**
     * A user's predictions for LIVE/FINISHED matches that have scores and an outcome.
     */
    private List<Prediction> scorablePredictionsForUser(User user) {
        List<Prediction> predictions;
        try {
            predictions = predictionRepository.findByUserWithMatch(user);
        } catch (Exception e) {
            log.warn("JOIN FETCH query failed for user {}, falling back to regular query: {}", user.getId(), e.getMessage());
            predictions = predictionRepository.findByUser(user);
        }

        return predictions.stream()
            .filter(p -> {
                try {
                    Match match = p.getMatch();
                    if (match == null) return false;
                    if (p.getPredictedOutcome() == null) return false;

                    MatchStatus status = match.getStatus();
                    if (status != MatchStatus.FINISHED && status != MatchStatus.LIVE) {
                        return false;
                    }
                    return match.getHomeScore() != null && match.getAwayScore() != null;
                } catch (Exception e) {
                    log.warn("Error processing prediction {}: {}", p.getId(), e.getMessage());
                    return false;
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Stored points for FINISHED matches; computed on-the-fly for LIVE matches.
     */
    private Integer pointsForDisplay(Prediction pred) {
        if (pred.getPoints() != null) {
            return pred.getPoints();
        }
        try {
            return computeGlobalPoints(pred);
        } catch (Exception e) {
            log.warn("Error computing points for prediction {}: {}", pred.getId(), e.getMessage());
            return null;
        }
    }
}
