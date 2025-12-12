package com.worldcup.service;

import com.worldcup.entity.Match;
import com.worldcup.entity.Prediction;
import com.worldcup.entity.User;
import com.worldcup.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final MatchService matchService;

    public Prediction createOrUpdatePrediction(User user, Long matchId, 
                                              Integer predictedHomeScore, 
                                              Integer predictedAwayScore) {
        Match match = matchService.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // Check if match is still open for predictions
        if (match.getStatus() != com.worldcup.entity.MatchStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot make predictions for matches that are not scheduled");
        }

        // Check if match time has passed (even if status is still SCHEDULED)
        if (match.getMatchDate() != null && 
            !match.getMatchDate().isAfter(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("Cannot make predictions for matches that have already started");
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
            .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        return predictionRepository.findByUserAndMatch(user, match);
    }

    public List<Prediction> findByUser(User user) {
        return predictionRepository.findByUser(user);
    }

    public List<Prediction> findByMatch(Long matchId) {
        Match match = matchService.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        return predictionRepository.findByMatch(match);
    }

    public Integer calculateTotalPoints(User user) {
        return predictionRepository.calculateTotalPointsByUser(user);
    }

    public void calculatePointsForMatch(Long matchId) {
        Match match = matchService.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            throw new IllegalStateException("Match result not available");
        }

        List<Prediction> predictions = predictionRepository.findByMatch(match);

        for (Prediction prediction : predictions) {
            int points = calculatePoints(
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore(),
                match.getHomeScore(),
                match.getAwayScore()
            );
            prediction.setPoints(points);
            predictionRepository.save(prediction);
        }
    }

    private int calculatePoints(int predictedHome, int predictedAway, 
                               int actualHome, int actualAway) {
        // Exact score match: 3 points
        if (predictedHome == actualHome && predictedAway == actualAway) {
            return 3;
        }

        // Correct winner (not exact score): 1 point
        boolean predictedHomeWins = predictedHome > predictedAway;
        boolean predictedAwayWins = predictedAway > predictedHome;
        boolean predictedDraw = predictedHome == predictedAway;

        boolean actualHomeWins = actualHome > actualAway;
        boolean actualAwayWins = actualAway > actualHome;
        boolean actualDraw = actualHome == actualAway;

        if (predictedHomeWins && actualHomeWins) return 1;
        if (predictedAwayWins && actualAwayWins) return 1;
        if (predictedDraw && actualDraw) return 1;

        // Wrong prediction: 0 points
        return 0;
    }
}

