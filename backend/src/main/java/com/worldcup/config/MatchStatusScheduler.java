package com.worldcup.config;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.repository.MatchRepository;
import com.worldcup.service.PredictionService;
import com.worldcup.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchStatusScheduler {

    private final MatchRepository matchRepository;
    private final WebSocketService webSocketService;
    private final PredictionService predictionService;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("MatchStatusScheduler component initialized and ready to run");
    }

    /**
     * Runs every 10 seconds to check and update match statuses
     * - SCHEDULED -> LIVE: When match date/time is reached or passed
     * - LIVE -> FINISHED: When match date/time + 2 hours is reached (assuming matches last ~2 hours)
     */
    @Scheduled(fixedRate = 10000) // Run every 10 seconds for more responsive status updates
    public void updateMatchStatuses() {
        try {
            // Use UTC for all time comparisons to match PostgreSQL's UTC storage
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

            log.info("=== SCHEDULER RUNNING - Current time (UTC): {} ===", now);

            // Update SCHEDULED matches to LIVE when match time is reached or passed
            List<Match> scheduledMatches = matchRepository.findByStatus(MatchStatus.SCHEDULED);
            log.info("Found {} scheduled matches to check", scheduledMatches.size());
            
            // Collect match IDs that need updating (to avoid concurrent modification)
            List<Long> matchesToUpdateToLive = new ArrayList<>();
            for (Match match : scheduledMatches) {
                if (match.getMatchDate() != null && !match.getMatchDate().isAfter(now)) {
                    matchesToUpdateToLive.add(match.getId());
                }
            }
            
            // Update each match in a separate transaction to avoid concurrent modification issues
            for (Long matchId : matchesToUpdateToLive) {
                updateMatchToLive(matchId, now);
            }

            // Update LIVE matches to FINISHED when match time + 2 hours is reached
            // Note: In practice, matches should be manually marked as FINISHED by admin
            // after setting the final scores. This scheduler is a fallback.
            List<Match> liveMatches = matchRepository.findByStatus(MatchStatus.LIVE);
            log.info("Found {} live matches to check", liveMatches.size());
            
            // Collect match IDs that need updating (to avoid concurrent modification)
            List<Long> matchesToUpdateToFinished = new ArrayList<>();
            for (Match match : liveMatches) {
                LocalDateTime matchEndTime = match.getMatchDate() != null 
                    ? match.getMatchDate().plusHours(2).plusMinutes(30)
                    : null;
                if (matchEndTime != null && !matchEndTime.isAfter(now)) {
                    matchesToUpdateToFinished.add(match.getId());
                }
            }
            
            // Update each match in a separate transaction to avoid concurrent modification issues
            for (Long matchId : matchesToUpdateToFinished) {
                updateMatchToFinished(matchId, now);
            }
        } catch (Exception e) {
            log.error("Error in scheduler: ", e);
        }
    }
    
    @Transactional
    private void updateMatchToLive(Long matchId, LocalDateTime now) {
        try {
            Match match = matchRepository.findById(matchId).orElse(null);
            if (match != null && match.getStatus() == MatchStatus.SCHEDULED) {
                log.info("Updating match {} from SCHEDULED to LIVE (match time: {}, current time: {})", 
                    matchId, match.getMatchDate(), now);
                MatchStatus oldStatus = match.getStatus();
                match.setStatus(MatchStatus.LIVE);
                matchRepository.save(match);
                log.info("Successfully updated match {} status to LIVE", matchId);
                // Broadcast update via WebSocket
                webSocketService.broadcastMatchStatusChange(matchId, oldStatus.name(), MatchStatus.LIVE.name());
            }
        } catch (Exception e) {
            log.error("Error updating match {} to LIVE: {}", matchId, e.getMessage(), e);
        }
    }
    
    @Transactional
    private void updateMatchToFinished(Long matchId, LocalDateTime now) {
        try {
            Match match = matchRepository.findById(matchId).orElse(null);
            if (match != null && match.getStatus() == MatchStatus.LIVE) {
                log.info("Updating match {} from LIVE to FINISHED (match time: {}, now: {})", 
                    matchId, match.getMatchDate(), now);
                MatchStatus oldStatus = match.getStatus();
                match.setStatus(MatchStatus.FINISHED);
                matchRepository.save(match);
                
                // Broadcast update via WebSocket
                webSocketService.broadcastMatchStatusChange(matchId, oldStatus.name(), MatchStatus.FINISHED.name());
                
                // Calculate points after transaction commits if match has scores
                if (match.getHomeScore() != null && match.getAwayScore() != null) {
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    try {
                                        log.info("Calculating points for finished match {} after transaction commit", matchId);
                                        predictionService.calculatePointsForMatch(matchId);
                                    } catch (Exception e) {
                                        log.error("Error calculating points for match {} after commit: {}", matchId, e.getMessage(), e);
                                    }
                                }
                            }
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error updating match {} to FINISHED: {}", matchId, e.getMessage(), e);
        }
    }
}

