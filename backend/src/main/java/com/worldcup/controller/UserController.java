package com.worldcup.controller;

import com.worldcup.dto.ChangePasswordRequest;
import com.worldcup.dto.LeaderboardEntryDTO;
import com.worldcup.dto.UpdateScreenNameRequest;
import com.worldcup.dto.UserProfileDTO;
import com.worldcup.entity.User;
import com.worldcup.repository.PredictionRepository;
import com.worldcup.repository.UserRepository;
import com.worldcup.security.CurrentUser;
import com.worldcup.service.PredictionService;
import com.worldcup.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUser currentUser;
    private final PredictionService predictionService;
    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;
    private final UserService userService;

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
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));
                    
                    long predictionCount = predictionRepository.findByUser(user).size();
                    
                    return new LeaderboardEntryDTO(
                            userId,
                            user.getEmail(),
                            user.getScreenName(),
                            totalPoints,
                            (int) predictionCount
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
}

