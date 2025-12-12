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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchStatusScheduler {

    private final MatchRepository matchRepository;
    private final PredictionService predictionService;
    private final WebSocketService webSocketService;

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
    @Transactional
    public void updateMatchStatuses() {
        try {
            // Use UTC for all time comparisons to match PostgreSQL's UTC storage
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime twoHoursAgo = now.minusHours(2);

            log.info("=== SCHEDULER RUNNING - Current time (UTC): {} ===", now);

            // Update SCHEDULED matches to LIVE when match time is reached or passed
            List<Match> scheduledMatches = matchRepository.findByStatus(MatchStatus.SCHEDULED);
            log.info("Found {} scheduled matches to check", scheduledMatches.size());
            
            for (Match match : scheduledMatches) {
                if (match.getMatchDate() != null) {
                    // Use isBefore with a small buffer (1 second) to account for any timing precision issues
                    // Also check if match time is equal or before now
                    boolean shouldUpdate = !match.getMatchDate().isAfter(now);
                    
                    log.info("Match {}: matchDate={}, now={}, isBefore={}, isEqual={}, isAfter={}, shouldUpdate={}", 
                        match.getId(), match.getMatchDate(), now, 
                        match.getMatchDate().isBefore(now), 
                        match.getMatchDate().isEqual(now),
                        match.getMatchDate().isAfter(now),
                        shouldUpdate);
                    
                    if (shouldUpdate) {
                        log.info("Updating match {} from SCHEDULED to LIVE (match time: {}, current time: {})", 
                            match.getId(), match.getMatchDate(), now);
                        MatchStatus oldStatus = match.getStatus();
                        match.setStatus(MatchStatus.LIVE);
                        matchRepository.save(match);
                        log.info("Successfully updated match {} status to LIVE", match.getId());
                        // Broadcast update via WebSocket
                        webSocketService.broadcastMatchStatusChange(match.getId(), oldStatus.name(), MatchStatus.LIVE.name());
                    }
                } else {
                    log.warn("Match {} has null matchDate", match.getId());
                }
            }

            // Update LIVE matches to FINISHED when match time + 2 hours is reached
            List<Match> liveMatches = matchRepository.findByStatus(MatchStatus.LIVE);
            log.info("Found {} live matches to check", liveMatches.size());
            
            for (Match match : liveMatches) {
                if (match.getMatchDate() != null && !match.getMatchDate().isAfter(twoHoursAgo)) {
                    log.info("Updating match {} from LIVE to FINISHED (match time: {}, twoHoursAgo: {})", 
                        match.getId(), match.getMatchDate(), twoHoursAgo);
                    MatchStatus oldStatus = match.getStatus();
                    match.setStatus(MatchStatus.FINISHED);
                    matchRepository.save(match);
                    
                    // Broadcast update via WebSocket
                    webSocketService.broadcastMatchStatusChange(match.getId(), oldStatus.name(), MatchStatus.FINISHED.name());
                    
                    // If match has scores, calculate points
                    if (match.getHomeScore() != null && match.getAwayScore() != null) {
                        try {
                            predictionService.calculatePointsForMatch(match.getId());
                            log.info("Calculated points for match {}", match.getId());
                            // Broadcast match update after points calculation
                            webSocketService.broadcastMatchUpdate(match.getId());
                        } catch (Exception e) {
                            log.error("Failed to calculate points for match {}: {}", match.getId(), e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in scheduler: ", e);
        }
    }
}

