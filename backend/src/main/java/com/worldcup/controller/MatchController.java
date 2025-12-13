package com.worldcup.controller;

import com.worldcup.dto.CreateMatchRequest;
import com.worldcup.dto.MatchDTO;
import com.worldcup.dto.UpdateMatchResultRequest;
import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.security.AdminRequired;
import com.worldcup.service.MatchService;
import com.worldcup.service.PredictionService;
import com.worldcup.service.WebSocketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchController {

    private final MatchService matchService;
    private final PredictionService predictionService;
    private final WebSocketService webSocketService;

    @GetMapping
    public ResponseEntity<List<MatchDTO>> getAllMatches(
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) String group) {
        List<Match> matches;
        
        if (status != null) {
            matches = matchService.findByStatus(status);
        } else if (group != null) {
            matches = matchService.findByGroup(group);
        } else {
            matches = matchService.findAll();
        }

        List<MatchDTO> matchDTOs = matches.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(matchDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDTO> getMatchById(@PathVariable Long id) {
        Match match = matchService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        return ResponseEntity.ok(convertToDTO(match));
    }

    @PostMapping
    @Transactional
    @AdminRequired
    public ResponseEntity<MatchDTO> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        Match match = matchService.createMatch(
                request.getHomeTeam(),
                request.getAwayTeam(),
                request.getMatchDate(),
                request.getVenue(),
                request.getGroup()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(match));
    }

    @PutMapping("/{id}/result")
    @Transactional
    @AdminRequired
    public ResponseEntity<MatchDTO> updateMatchResult(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMatchResultRequest request) {
        Match match = matchService.updateMatchResult(
                id,
                request.getHomeScore(),
                request.getAwayScore()
        );

        // Calculate points for all predictions of this match
        predictionService.calculatePointsForMatch(id);

        // Broadcast update via WebSocket
        webSocketService.broadcastMatchUpdate(id);

        return ResponseEntity.ok(convertToDTO(match));
    }

    @PutMapping("/{id}/status")
    @Transactional
    @AdminRequired
    public ResponseEntity<MatchDTO> updateMatchStatus(
            @PathVariable Long id,
            @RequestParam MatchStatus status) {
        Match match = matchService.updateMatchStatus(id, status);
        
        // Automatically calculate points when match is finished and has scores
        if (status == MatchStatus.FINISHED && match.getHomeScore() != null && match.getAwayScore() != null) {
            try {
                predictionService.calculatePointsForMatch(id);
            } catch (IllegalStateException e) {
                // Points calculation failed (e.g., no scores yet) - that's okay
                // The match status was still updated successfully
            }
        }
        
        // Broadcast update via WebSocket
        webSocketService.broadcastMatchUpdate(id);
        
        return ResponseEntity.ok(convertToDTO(match));
    }

    @PostMapping("/{id}/calculate-points")
    @Transactional
    public ResponseEntity<String> calculatePointsForMatch(@PathVariable Long id) {
        try {
            predictionService.calculatePointsForMatch(id);
            return ResponseEntity.ok("Points calculated successfully for match " + id);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private MatchDTO convertToDTO(Match match) {
        return new MatchDTO(
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
        );
    }
}

