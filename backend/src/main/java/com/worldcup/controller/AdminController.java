package com.worldcup.controller;

import com.worldcup.dto.CreateMatchRequest;
import com.worldcup.dto.MatchDTO;
import com.worldcup.dto.UpdateMatchResultRequest;
import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.entity.Role;
import com.worldcup.entity.User;
import com.worldcup.repository.UserRepository;
import com.worldcup.security.AdminRequired;
import com.worldcup.config.FootballApiSyncScheduler;
import com.worldcup.service.MatchService;
import com.worldcup.service.PredictionService;
import com.worldcup.service.WebSocketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@AdminRequired
@Slf4j
public class AdminController {

    private final MatchService matchService;
    private final UserRepository userRepository;
    private final PredictionService predictionService;
    private final WebSocketService webSocketService;
    private final FootballApiSyncScheduler footballApiSyncScheduler;

    @GetMapping("/users")
    public ResponseEntity<List<UserInfoDTO>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserInfoDTO> userDTOs = users.stream()
                .map(user -> new UserInfoDTO(
                        user.getId(),
                        user.getEmail(),
                        user.getScreenName(),
                        user.getRole().name(),
                        user.getEnabled(),
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    @PutMapping("/users/{id}/role")
    @Transactional
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        try {
            user.setRole(Role.valueOf(role.toUpperCase()));
            userRepository.save(user);
            return ResponseEntity.ok().body(java.util.Map.of("message", "User role updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid role: " + role));
        }
    }

    @PutMapping("/users/{id}/enabled")
    @Transactional
    public ResponseEntity<?> updateUserEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setEnabled(enabled);
        userRepository.save(user);
        return ResponseEntity.ok().body(java.util.Map.of("message", "User enabled status updated successfully"));
    }

    @PostMapping("/matches")
    @Transactional
    public ResponseEntity<MatchDTO> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        Match match = matchService.createMatch(
                request.getHomeTeam(),
                request.getAwayTeam(),
                request.getMatchDate(),
                request.getVenue(),
                request.getGroup()
        );
        // Broadcast new match via WebSocket
        webSocketService.broadcastMatchUpdate(match.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(match));
    }

    @PutMapping("/matches/{id}/result")
    @Transactional
    public ResponseEntity<MatchDTO> updateMatchResult(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMatchResultRequest request) {
        try {
            Match match = matchService.updateMatchResult(
                    id,
                    request.getHomeScore(),
                    request.getAwayScore()
            );
            
            // Calculate points for all predictions of this match
            // Note: This is also handled by MatchEntityListener, but we call it explicitly
            // to ensure it happens immediately. The method is idempotent, so calling it twice is safe.
            try {
                predictionService.calculatePointsForMatch(id);
            } catch (Exception e) {
                // Log error but don't fail the request - match result was updated successfully
                // Points will be calculated by MatchEntityListener or can be recalculated later
                log.error("Error calculating points for match {}: {}", id, e.getMessage(), e);
            }
            
            // Broadcast match update via WebSocket
            webSocketService.broadcastMatchUpdate(id);
            return ResponseEntity.ok(convertToDTO(match));
        } catch (IllegalArgumentException e) {
            log.error("Match not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            log.error("Error updating match result for match {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PutMapping("/matches/{id}/status")
    @Transactional
    public ResponseEntity<MatchDTO> updateMatchStatus(
            @PathVariable Long id,
            @RequestParam MatchStatus status) {
        Match match = matchService.updateMatchStatus(id, status);
        
        // Note: Points calculation is automatically handled by MatchEntityListener
        // when match status is updated to FINISHED and has scores.
        // No need to calculate manually here to avoid concurrent modification issues.
        
        // Broadcast match status change via WebSocket
        webSocketService.broadcastMatchUpdate(id);
        
        return ResponseEntity.ok(convertToDTO(match));
    }

    @DeleteMapping("/matches/{id}")
    @Transactional
    public ResponseEntity<?> deleteMatch(@PathVariable Long id) {
        matchService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        matchService.deleteMatch(id);
        return ResponseEntity.ok().body(java.util.Map.of("message", "Match deleted successfully"));
    }

    @PostMapping("/sync/fixtures")
    public ResponseEntity<?> triggerFixtureSync() {
        try {
            footballApiSyncScheduler.syncFixturesInternal();
            return ResponseEntity.ok().body(java.util.Map.of("message", "Fixture sync triggered successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to trigger sync: " + e.getMessage()));
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

    // Inner DTO class
    public static class UserInfoDTO {
        private Long id;
        private String email;
        private String screenName;
        private String role;
        private Boolean enabled;
        private java.time.LocalDateTime createdAt;

        public UserInfoDTO(Long id, String email, String screenName, String role, Boolean enabled, java.time.LocalDateTime createdAt) {
            this.id = id;
            this.email = email;
            this.screenName = screenName;
            this.role = role;
            this.enabled = enabled;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getScreenName() { return screenName; }
        public void setScreenName(String screenName) { this.screenName = screenName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}

