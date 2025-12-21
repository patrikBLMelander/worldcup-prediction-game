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
import com.worldcup.entity.Notification;
import com.worldcup.service.MatchService;
import com.worldcup.service.NotificationService;
import com.worldcup.service.PredictionService;
import com.worldcup.service.WebSocketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private final NotificationService notificationService;

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
            log.info("Updating match result for match {}: {} - {}", id, request.getHomeScore(), request.getAwayScore());
            
            Match match = matchService.updateMatchResult(
                    id,
                    request.getHomeScore(),
                    request.getAwayScore()
            );
            
            log.info("Match {} updated successfully, converting to DTO", id);
            
            // Convert to DTO within the transaction to avoid lazy loading issues
            MatchDTO matchDTO = convertToDTO(match);
            
            log.info("DTO converted successfully for match {}", id);
            
            // Calculate points after transaction commits to avoid conflicts
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                log.info("Transaction committed, calculating points for match {}", id);
                                predictionService.calculatePointsForMatch(id);
                                log.info("Calculated points for match {} after transaction commit", id);
                            } catch (Exception e) {
                                log.error("Error calculating points for match {} after commit: {}", id, e.getMessage(), e);
                                e.printStackTrace();
                            }
                        }
                    }
                );
            } else {
                log.warn("No active transaction when trying to register synchronization for match {}", id);
            }
            
            // Broadcast match update via WebSocket
            try {
                webSocketService.broadcastMatchUpdate(id);
                log.info("WebSocket broadcast sent for match {}", id);
            } catch (Exception e) {
                log.error("Error broadcasting match update for match {}: {}", id, e.getMessage(), e);
                // Don't fail the request if WebSocket broadcast fails
            }
            
            log.info("Successfully updated match result for match {}", id);
            return ResponseEntity.ok(matchDTO);
        } catch (IllegalArgumentException e) {
            log.error("Match not found: {}", id, e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            log.error("Error updating match result for match {}: {}", id, e.getMessage(), e);
            e.printStackTrace();
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

    @PostMapping("/matches/{id}/recalculate-points")
    @Transactional
    public ResponseEntity<?> recalculatePointsForMatch(@PathVariable Long id) {
        try {
            Match match = matchService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Match not found"));
            
            if (match.getStatus() != MatchStatus.FINISHED) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", "Match must be FINISHED to recalculate points. Current status: " + match.getStatus()
                ));
            }
            
            if (match.getHomeScore() == null || match.getAwayScore() == null) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", "Match must have both home and away scores to calculate points"
                ));
            }
            
            log.info("Admin recalculating points for match {}: {} vs {}", 
                    id, match.getHomeTeam(), match.getAwayTeam());
            
            predictionService.calculatePointsForMatch(id);
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "message", "Points recalculated successfully for match " + id,
                "match", match.getHomeTeam() + " vs " + match.getAwayTeam(),
                "score", match.getHomeScore() + " - " + match.getAwayScore()
            ));
        } catch (IllegalArgumentException e) {
            log.error("Match not found: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Cannot recalculate points: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error recalculating points for match {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to recalculate points: " + e.getMessage()));
        }
    }

    @PostMapping("/matches/recalculate-all-finished")
    @Transactional
    public ResponseEntity<?> recalculateAllFinishedMatches() {
        try {
            log.info("Admin recalculating points for all FINISHED matches");
            
            List<Match> finishedMatches = matchService.findByStatus(MatchStatus.FINISHED);
            int successCount = 0;
            int errorCount = 0;
            List<String> errors = new java.util.ArrayList<>();
            
            for (Match match : finishedMatches) {
                if (match.getHomeScore() == null || match.getAwayScore() == null) {
                    log.debug("Skipping match {} - no scores available", match.getId());
                    continue;
                }
                
                try {
                    predictionService.calculatePointsForMatch(match.getId());
                    successCount++;
                    log.debug("Recalculated points for match {}: {} vs {}", 
                            match.getId(), match.getHomeTeam(), match.getAwayTeam());
                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = String.format("Match %d (%s vs %s): %s", 
                            match.getId(), match.getHomeTeam(), match.getAwayTeam(), e.getMessage());
                    errors.add(errorMsg);
                    log.error("Error recalculating points for match {}: {}", match.getId(), e.getMessage());
                }
            }
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "message", "Points recalculation completed",
                "totalMatches", finishedMatches.size(),
                "successCount", successCount,
                "errorCount", errorCount,
                "errors", errors
            ));
        } catch (Exception e) {
            log.error("Error recalculating points for all finished matches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to recalculate points: " + e.getMessage()));
        }
    }

    @PostMapping("/test/notification")
    public ResponseEntity<?> testNotification(@RequestParam(required = false) Long userId) {
        try {
            User targetUser;
            if (userId != null) {
                targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            } else {
                // Use current user if no userId provided
                targetUser = userRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No users found"));
            }
            
            notificationService.sendNotification(
                targetUser,
                Notification.NotificationType.ACHIEVEMENT,
                "Test Notification",
                "This is a test notification to verify the notification system is working!",
                "🧪",
                "/profile"
            );
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "message", "Test notification sent to user: " + targetUser.getEmail(),
                "userId", targetUser.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to send test notification: " + e.getMessage()));
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

