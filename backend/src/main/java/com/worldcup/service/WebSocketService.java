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
        MatchDTO dto = new MatchDTO();
        dto.setId(match.getId());
        dto.setHomeTeam(match.getHomeTeam());
        dto.setAwayTeam(match.getAwayTeam());
        dto.setMatchDate(match.getMatchDate());
        dto.setVenue(match.getVenue());
        dto.setGroup(match.getGroup());
        dto.setStatus(match.getStatus());
        dto.setHomeScore(match.getHomeScore());
        dto.setAwayScore(match.getAwayScore());
        return dto;
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
}

