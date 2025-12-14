package com.worldcup.config;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.repository.MatchRepository;
import com.worldcup.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

            log.info("=== SCHEDULER RUNNING - Current time (UTC): {} ===", now);

            // Update SCHEDULED matches to LIVE when match time is reached or passed
            List<Match> scheduledMatches = matchRepository.findByStatus(MatchStatus.SCHEDULED);
            log.info("Found {} scheduled matches to check", scheduledMatches.size());
            
            // Create a copy to avoid concurrent modification issues
            List<Match> scheduledMatchesCopy = new ArrayList<>(scheduledMatches);
            
            for (Match match : scheduledMatchesCopy) {
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
            // Note: In practice, matches should be manually marked as FINISHED by admin
            // after setting the final scores. This scheduler is a fallback.
            List<Match> liveMatches = matchRepository.findByStatus(MatchStatus.LIVE);
            log.info("Found {} live matches to check", liveMatches.size());
            
            // Create a copy to avoid concurrent modification issues
            List<Match> liveMatchesCopy = new ArrayList<>(liveMatches);
            
            for (Match match : liveMatchesCopy) {
                // Mark as FINISHED if match time + 2.5 hours has passed (allowing for extra time)
                LocalDateTime matchEndTime = match.getMatchDate() != null 
                    ? match.getMatchDate().plusHours(2).plusMinutes(30)
                    : null;
                
                if (matchEndTime != null && !matchEndTime.isAfter(now)) {
                    log.info("Updating match {} from LIVE to FINISHED (match time: {}, match end time: {}, now: {})", 
                        match.getId(), match.getMatchDate(), matchEndTime, now);
                    MatchStatus oldStatus = match.getStatus();
                    match.setStatus(MatchStatus.FINISHED);
                    matchRepository.save(match);
                    
                    // Broadcast update via WebSocket
                    webSocketService.broadcastMatchStatusChange(match.getId(), oldStatus.name(), MatchStatus.FINISHED.name());
                    
                    // Note: Points calculation is automatically handled by MatchEntityListener
                    // when match is saved with FINISHED status and has scores
                }
            }
        } catch (Exception e) {
            log.error("Error in scheduler: ", e);
        }
    }
}

