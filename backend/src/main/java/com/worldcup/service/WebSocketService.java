package com.worldcup.service;

import com.worldcup.dto.MatchDTO;
import com.worldcup.entity.Match;
import com.worldcup.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchRepository matchRepository;

    /**
     * Convert Match entity to MatchDTO
     */
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

    /**
     * Broadcast match update to all connected clients
     */
    public void broadcastMatchUpdate(Long matchId) {
        matchRepository.findById(matchId).ifPresent(match -> {
            MatchDTO matchDTO = convertToDTO(match);
            messagingTemplate.convertAndSend("/topic/matches/update", matchDTO);
            log.debug("Broadcasted match update for match {}", matchId);
        });
    }

    /**
     * Broadcast match status change
     */
    public void broadcastMatchStatusChange(Long matchId, String oldStatus, String newStatus) {
        matchRepository.findById(matchId).ifPresent(match -> {
            MatchDTO matchDTO = convertToDTO(match);
            messagingTemplate.convertAndSend("/topic/matches/status", matchDTO);
            log.info("Broadcasted match status change for match {}: {} -> {}", matchId, oldStatus, newStatus);
        });
    }

    /**
     * Send notification to specific user via WebSocket
     * Uses Spring's user-specific messaging (automatically routes to /user/{username}/queue/notifications)
     */
    public void sendNotificationToUser(String username, Object notification) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
        log.debug("Sent notification to user {} via WebSocket", username);
    }
}

