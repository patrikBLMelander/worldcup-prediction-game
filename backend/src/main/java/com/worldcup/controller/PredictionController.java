package com.worldcup.controller;

import com.worldcup.dto.CreatePredictionRequest;
import com.worldcup.dto.PredictionDTO;
import com.worldcup.entity.Match;
import com.worldcup.entity.Prediction;
import com.worldcup.entity.User;
import com.worldcup.security.CurrentUser;
import com.worldcup.service.AchievementService;
import com.worldcup.service.PredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;
    private final AchievementService achievementService;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<PredictionDTO> createOrUpdatePrediction(
            @Valid @RequestBody CreatePredictionRequest request) {
        User user = currentUser.getCurrentUserOrThrow();

        Prediction prediction = predictionService.createOrUpdatePrediction(
                user,
                request.getMatchId(),
                request.getPredictedHomeScore(),
                request.getPredictedAwayScore()
        );

        // Check for achievements after prediction is made
        achievementService.checkAchievementsAfterPrediction(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(prediction));
    }

    @GetMapping("/my-predictions")
    public ResponseEntity<List<PredictionDTO>> getMyPredictions() {
        User user = currentUser.getCurrentUserOrThrow();
        List<Prediction> predictions = predictionService.findByUser(user);

        List<PredictionDTO> predictionDTOs = predictions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(predictionDTOs);
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<PredictionDTO> getMyPredictionForMatch(@PathVariable Long matchId) {
        User user = currentUser.getCurrentUserOrThrow();
        
        return predictionService.findByUserAndMatch(user, matchId)
                .map(prediction -> ResponseEntity.ok(convertToDTO(prediction)))
                .orElse(ResponseEntity.notFound().build());
    }

    private PredictionDTO convertToDTO(Prediction prediction) {
        Match match = prediction.getMatch();
        return new PredictionDTO(
                prediction.getId(),
                match.getId(),
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getMatchDate(),
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore(),
                prediction.getPoints(),
                prediction.getCreatedAt(),
                prediction.getUpdatedAt()
        );
    }
}


