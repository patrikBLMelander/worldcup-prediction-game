package com.worldcup.config;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.repository.MatchRepository;
import com.worldcup.service.FootballApiService;
import com.worldcup.service.PredictionService;
import com.worldcup.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scheduled job to sync matches from Football API
 * - Fetches fixtures for upcoming matches
 * - Updates live scores for matches in progress
 * - Updates finished match results
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FootballApiSyncScheduler {

    private final FootballApiService footballApiService;
    private final MatchRepository matchRepository;
    private final WebSocketService webSocketService;
    private final PredictionService predictionService;

    @Value("${football.api.enabled:false}")
    private boolean apiEnabled;

    /**
     * Sync fixtures - runs every hour
     * Fetches matches for the next 7 days
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void syncFixtures() {
        syncFixturesInternal();
    }

    /**
     * Internal method that can be called manually
     */
    @Transactional
    public void syncFixturesInternal() {
        if (!apiEnabled) {
            return;
        }

        try {
            log.info("Syncing fixtures from Football API...");
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime endDate = now.plusDays(7);

            List<FootballApiService.MatchData> apiMatches = footballApiService.fetchMatches(now, endDate);

            // Group existing matches by external API ID
            // Only fetch matches with external API IDs to avoid loading all matches into memory
            Map<String, Match> existingMatches = matchRepository.findAllWithExternalApiId().stream()
                    .collect(Collectors.toMap(Match::getExternalApiId, m -> m));

            int created = 0;
            int updated = 0;

            for (FootballApiService.MatchData apiMatch : apiMatches) {
                String externalId = String.valueOf(apiMatch.id);
                Match existingMatch = existingMatches.get(externalId);

                if (existingMatch != null) {
                    // Update existing match
                    footballApiService.updateMatchFromApi(existingMatch, apiMatch);
                    matchRepository.save(existingMatch);
                    updated++;
                    
                    // Broadcast update via WebSocket
                    webSocketService.broadcastMatchUpdate(existingMatch.getId());
                } else {
                    // Create new match
                    Match newMatch = footballApiService.convertToMatch(apiMatch);
                    Match saved = matchRepository.save(newMatch);
                    created++;
                    log.debug("Created new match: {} vs {}", saved.getHomeTeam(), saved.getAwayTeam());
                }
            }

            log.info("Fixture sync completed: {} created, {} updated", created, updated);
        } catch (Exception e) {
            log.error("Error syncing fixtures from Football API: {}", e.getMessage(), e);
        }
    }

    /**
     * Sync live scores - runs every 60 seconds during match days
     * Fetches matches that are currently live
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void syncLiveScores() {
        if (!apiEnabled) {
            return;
        }

        try {
            List<FootballApiService.MatchData> liveMatches = footballApiService.fetchLiveMatches();

            if (liveMatches.isEmpty()) {
                return;
            }

            log.info("Syncing {} live matches from Football API...", liveMatches.size());

            // Group existing matches by external API ID
            // Only fetch matches with external API IDs to avoid loading all matches into memory
            Map<String, Match> existingMatches = matchRepository.findAllWithExternalApiId().stream()
                    .collect(Collectors.toMap(Match::getExternalApiId, m -> m));

            for (FootballApiService.MatchData apiMatch : liveMatches) {
                String externalId = String.valueOf(apiMatch.id);
                Match existingMatch = existingMatches.get(externalId);

                if (existingMatch != null) {
                    MatchStatus oldStatus = existingMatch.getStatus();
                    Integer oldHomeScore = existingMatch.getHomeScore();
                    Integer oldAwayScore = existingMatch.getAwayScore();

                    // Update match with live data
                    footballApiService.updateMatchFromApi(existingMatch, apiMatch);
                    matchRepository.save(existingMatch);

                    // Broadcast update if status or scores changed
                    boolean changed = !existingMatch.getStatus().equals(oldStatus) ||
                            !java.util.Objects.equals(existingMatch.getHomeScore(), oldHomeScore) ||
                            !java.util.Objects.equals(existingMatch.getAwayScore(), oldAwayScore);

                    if (changed) {
                        webSocketService.broadcastMatchUpdate(existingMatch.getId());
                        log.debug("Updated live match: {} vs {} - {}:{}", 
                                existingMatch.getHomeTeam(), existingMatch.getAwayTeam(),
                                existingMatch.getHomeScore(), existingMatch.getAwayScore());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error syncing live scores from Football API: {}", e.getMessage(), e);
        }
    }

    /**
     * Sync finished matches - runs every 5 minutes
     * Updates scores for recently finished matches
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void syncFinishedMatches() {
        if (!apiEnabled) {
            return;
        }

        try {
            // Fetch matches from the last 24 hours
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime startDate = now.minusDays(1);

            List<FootballApiService.MatchData> apiMatches = footballApiService.fetchMatches(startDate, now);

            // Filter for finished matches
            List<FootballApiService.MatchData> finishedMatches = apiMatches.stream()
                    .filter(m -> "FINISHED".equals(m.status))
                    .toList();

            if (finishedMatches.isEmpty()) {
                return;
            }

            log.info("Syncing {} finished matches from Football API...", finishedMatches.size());

            // Group existing matches by external API ID
            // Only fetch matches with external API IDs to avoid loading all matches into memory
            Map<String, Match> existingMatches = matchRepository.findAllWithExternalApiId().stream()
                    .collect(Collectors.toMap(Match::getExternalApiId, m -> m));

            for (FootballApiService.MatchData apiMatch : finishedMatches) {
                String externalId = String.valueOf(apiMatch.id);
                Match existingMatch = existingMatches.get(externalId);

                if (existingMatch != null && existingMatch.getStatus() != MatchStatus.FINISHED) {
                    // Update match to finished status with final scores
                    footballApiService.updateMatchFromApi(existingMatch, apiMatch);
                    matchRepository.save(existingMatch);
                    
                    // Calculate points after transaction commits if match has scores
                    Long matchId = existingMatch.getId();
                    if (existingMatch.getHomeScore() != null && existingMatch.getAwayScore() != null) {
                        if (TransactionSynchronizationManager.isActualTransactionActive()) {
                            TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        try {
                                            log.info("Calculating points for finished match {} from API sync after transaction commit", matchId);
                                            predictionService.calculatePointsForMatch(matchId);
                                        } catch (Exception e) {
                                            log.error("Error calculating points for match {} after commit: {}", matchId, e.getMessage(), e);
                                        }
                                    }
                                }
                            );
                        }
                    }
                    
                    // Broadcast update via WebSocket
                    webSocketService.broadcastMatchUpdate(existingMatch.getId());
                    
                    log.debug("Updated finished match: {} vs {} - {}:{}", 
                            existingMatch.getHomeTeam(), existingMatch.getAwayTeam(),
                            existingMatch.getHomeScore(), existingMatch.getAwayScore());
                }
            }
        } catch (Exception e) {
            log.error("Error syncing finished matches from Football API: {}", e.getMessage(), e);
        }
    }
}


