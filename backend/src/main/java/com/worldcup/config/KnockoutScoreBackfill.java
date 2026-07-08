package com.worldcup.config;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.repository.MatchRepository;
import com.worldcup.service.FootballApiService;
import com.worldcup.service.FootballApiService.MatchData;
import com.worldcup.service.PredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * One-time (idempotent) fix for knockout matches whose stored result included
 * extra-time or penalty goals.
 *
 * <p>Earlier syncs wrote football-data.org's {@code fullTime} score into
 * {@code homeScore}/{@code awayScore}. For matches decided after 90 minutes that
 * aggregate includes extra time and penalties, so e.g. a 1-1 draw that went to
 * penalties was stored (and scored) as a decisive result. Scoring now uses the
 * regulation score; this backfill re-applies scores for already-FINISHED matches
 * and recomputes prediction points wherever the regulation result differs from
 * what was stored.
 *
 * <p>Runs after {@link PredictionOutcomeBackfill}. Safe on every startup: once
 * scores are corrected the comparison is a no-op and nothing is rewritten.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class KnockoutScoreBackfill implements ApplicationRunner {

    private final MatchRepository matchRepository;
    private final FootballApiService footballApiService;
    private final PredictionService predictionService;

    @Value("${football.api.enabled:false}")
    private boolean apiEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!apiEnabled) {
            return;
        }
        try {
            backfill();
        } catch (Exception e) {
            log.error("Knockout score backfill failed: {}", e.getMessage(), e);
        }
    }

    private void backfill() {
        var apiMatches = footballApiService.fetchAllMatches();
        if (apiMatches.isEmpty()) {
            return;
        }

        Map<String, Match> byExternalId = matchRepository.findAllWithExternalApiId().stream()
                .collect(Collectors.toMap(Match::getExternalApiId, m -> m));

        int corrected = 0;
        for (MatchData api : apiMatches) {
            if (!"FINISHED".equals(api.status)) {
                continue;
            }
            Match match = byExternalId.get(String.valueOf(api.id));
            if (match == null || match.getStatus() != MatchStatus.FINISHED) {
                continue;
            }

            Integer oldHome = match.getHomeScore();
            Integer oldAway = match.getAwayScore();
            String oldDuration = match.getDuration();

            // Re-apply from the API; this now writes the regulation score plus the
            // extra-time / penalty display fields.
            footballApiService.updateMatchFromApi(match, api);

            boolean scoreChanged = !Objects.equals(oldHome, match.getHomeScore())
                    || !Objects.equals(oldAway, match.getAwayScore());
            boolean anythingChanged = scoreChanged
                    || !Objects.equals(oldDuration, match.getDuration());

            if (!anythingChanged) {
                continue;
            }

            matchRepository.save(match);

            if (scoreChanged) {
                log.info("Backfill corrected match {} ({} vs {}): {}-{} -> {}-{} (recomputing points)",
                        match.getId(), match.getHomeTeam(), match.getAwayTeam(),
                        oldHome, oldAway, match.getHomeScore(), match.getAwayScore());
                predictionService.calculatePointsForMatch(match.getId());
                corrected++;
            }
        }

        if (corrected > 0) {
            log.info("Knockout score backfill: corrected regulation result for {} match(es)", corrected);
        }
    }
}
