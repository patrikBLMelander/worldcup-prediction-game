package com.worldcup.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to fetch real Premier League match data from Football-Data.org API
 * Free tier: 10 requests per minute
 * Premier League competition ID: 2021
 */
@Slf4j
@Service
public class FootballApiService {

    private final RestTemplate restTemplate;

    public FootballApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${football.api.enabled:false}")
    private boolean apiEnabled;

    @Value("${football.api.key:}")
    private String apiKey;

    @Value("${football.api.base-url:https://api.football-data.org/v4}")
    private String baseUrl;

    @Value("${football.api.competition-id:2021}")
    private String competitionId; // Premier League = 2021

    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Fetch matches for a date range
     */
    public List<MatchData> fetchMatches(LocalDateTime from, LocalDateTime to) {
        if (!apiEnabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("Football API is not enabled or API key is missing");
            return new ArrayList<>();
        }

        try {
            String fromStr = from.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String toStr = to.format(DateTimeFormatter.ISO_LOCAL_DATE);

            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/competitions/" + competitionId + "/matches")
                    .queryParam("dateFrom", fromStr)
                    .queryParam("dateTo", toStr)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("Fetching matches from Football API: {} to {}", fromStr, toStr);
            ResponseEntity<MatchesResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, MatchesResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} matches from API", response.getBody().matches.size());
                return response.getBody().matches;
            } else {
                log.warn("Failed to fetch matches: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Error fetching matches from Football API: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Fetch all matches for the configured competition (no date filter).
     * Used for cup-style competitions like the World Cup where the whole
     * tournament fits in one response (~104 matches).
     */
    public List<MatchData> fetchAllMatches() {
        if (!apiEnabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("Football API is not enabled or API key is missing");
            return new ArrayList<>();
        }

        try {
            String url = baseUrl + "/competitions/" + competitionId + "/matches";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("Fetching all matches for competition {}", competitionId);
            ResponseEntity<MatchesResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, MatchesResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} matches from API", response.getBody().matches.size());
                return response.getBody().matches;
            } else {
                log.warn("Failed to fetch matches: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Error fetching matches from Football API: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Map API stage/group to a human-readable label stored in Match.group.
     * Group stage uses the group letter ("Group A"); knockout rounds use the
     * stage name ("Round of 16", "Quarter-Final", etc.).
     */
    private String stageLabel(MatchData m) {
        if (m.group != null && !m.group.isBlank()) {
            // e.g. "GROUP_A" -> "Group A"
            String letter = m.group.replace("GROUP_", "");
            return "Group " + letter;
        }
        if (m.stage == null) {
            return null;
        }
        return switch (m.stage) {
            case "GROUP_STAGE" -> "Group Stage";
            case "LAST_32" -> "Round of 32";
            case "LAST_16" -> "Round of 16";
            case "QUARTER_FINALS" -> "Quarter-Final";
            case "SEMI_FINALS" -> "Semi-Final";
            case "THIRD_PLACE" -> "Third-Place Play-off";
            case "FINAL" -> "Final";
            default -> m.stage;
        };
    }

    /**
     * Fetch live matches (matches in progress)
     */
    public List<MatchData> fetchLiveMatches() {
        if (!apiEnabled || apiKey == null || apiKey.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String url = baseUrl + "/competitions/" + competitionId + "/matches?status=LIVE";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<MatchesResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, MatchesResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} live matches from API", response.getBody().matches.size());
                return response.getBody().matches;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching live matches from Football API: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert API match data to our Match entity
     */
    public Match convertToMatch(MatchData apiMatch) {
        Match match = new Match();
        match.setExternalApiId(String.valueOf(apiMatch.id));

        // Knockout matches arrive with null teams until the bracket resolves.
        // Match.homeTeam/awayTeam are @NotBlank, so use "TBD" as a placeholder;
        // updateMatchFromApi will fill in real names on a later sync.
        if (apiMatch.homeTeam != null && apiMatch.homeTeam.name != null) {
            match.setHomeTeam(apiMatch.homeTeam.name);
            match.setHomeTeamCrest(apiMatch.homeTeam.crest);
        } else {
            match.setHomeTeam("TBD");
        }

        if (apiMatch.awayTeam != null && apiMatch.awayTeam.name != null) {
            match.setAwayTeam(apiMatch.awayTeam.name);
            match.setAwayTeamCrest(apiMatch.awayTeam.crest);
        } else {
            match.setAwayTeam("TBD");
        }

        // Convert UTC date to LocalDateTime
        if (apiMatch.utcDate != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(apiMatch.utcDate, API_DATE_FORMATTER);
            match.setMatchDate(zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime());
        }

        match.setVenue(apiMatch.venue != null ? apiMatch.venue : "TBD");
        match.setGroup(stageLabel(apiMatch));
        
        // Map API status to our status
        if (apiMatch.status != null) {
            switch (apiMatch.status) {
                case "SCHEDULED", "TIMED" -> match.setStatus(MatchStatus.SCHEDULED);
                case "LIVE", "IN_PLAY", "PAUSED" -> match.setStatus(MatchStatus.LIVE);
                case "FINISHED" -> match.setStatus(MatchStatus.FINISHED);
                case "POSTPONED", "CANCELLED", "SUSPENDED" -> match.setStatus(MatchStatus.CANCELLED);
                default -> match.setStatus(MatchStatus.SCHEDULED);
            }
        } else {
            match.setStatus(MatchStatus.SCHEDULED);
        }
        
        // Set scores if available
        if (apiMatch.score != null) {
            if (apiMatch.score.fullTime != null) {
                match.setHomeScore(apiMatch.score.fullTime.home);
                match.setAwayScore(apiMatch.score.fullTime.away);
            } else if (apiMatch.score.halfTime != null) {
                // Use half-time score if full-time not available (for live matches)
                match.setHomeScore(apiMatch.score.halfTime.home);
                match.setAwayScore(apiMatch.score.halfTime.away);
            }
        }
        
        return match;
    }

    /**
     * Update existing match with API data
     */
    public void updateMatchFromApi(Match existingMatch, MatchData apiMatch) {
        // Update status
        if (apiMatch.status != null) {
            switch (apiMatch.status) {
                case "SCHEDULED", "TIMED" -> existingMatch.setStatus(MatchStatus.SCHEDULED);
                case "LIVE", "IN_PLAY", "PAUSED" -> existingMatch.setStatus(MatchStatus.LIVE);
                case "FINISHED" -> existingMatch.setStatus(MatchStatus.FINISHED);
                case "POSTPONED", "CANCELLED", "SUSPENDED" -> existingMatch.setStatus(MatchStatus.CANCELLED);
            }
        }
        
        // Update teams + crests when the API has them (knockout slots arrive
        // as null and get filled in once bracket placements resolve).
        if (apiMatch.homeTeam != null && apiMatch.homeTeam.name != null) {
            existingMatch.setHomeTeam(apiMatch.homeTeam.name);
            if (apiMatch.homeTeam.crest != null) {
                existingMatch.setHomeTeamCrest(apiMatch.homeTeam.crest);
            }
        }
        if (apiMatch.awayTeam != null && apiMatch.awayTeam.name != null) {
            existingMatch.setAwayTeam(apiMatch.awayTeam.name);
            if (apiMatch.awayTeam.crest != null) {
                existingMatch.setAwayTeamCrest(apiMatch.awayTeam.crest);
            }
        }

        // Update group/stage label (e.g. when API moves a match between rounds)
        existingMatch.setGroup(stageLabel(apiMatch));
        
        // Update scores
        if (apiMatch.score != null) {
            if (apiMatch.score.fullTime != null) {
                existingMatch.setHomeScore(apiMatch.score.fullTime.home);
                existingMatch.setAwayScore(apiMatch.score.fullTime.away);
            } else if (apiMatch.score.halfTime != null && existingMatch.getStatus() == MatchStatus.LIVE) {
                // Update with half-time score for live matches
                existingMatch.setHomeScore(apiMatch.score.halfTime.home);
                existingMatch.setAwayScore(apiMatch.score.halfTime.away);
            }
        }
        
        // Update match date if changed (for postponed matches)
        if (apiMatch.utcDate != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(apiMatch.utcDate, API_DATE_FORMATTER);
            existingMatch.setMatchDate(zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime());
        }
    }

    // DTOs for API response
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchesResponse {
        private List<MatchData> matches = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchData {
        public Long id;
        public String status;
        public String utcDate;
        public String stage; // e.g. GROUP_STAGE, LAST_16, QUARTER_FINALS, FINAL
        public String group; // e.g. GROUP_A (only set for GROUP_STAGE)
        public TeamData homeTeam;
        public TeamData awayTeam;
        public ScoreData score;
        public String venue;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamData {
        private String name;
        private String shortName;
        private String crest; // Team logo/crest URL from API
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreData {
        private ScoreDetail fullTime;
        private ScoreDetail halfTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreDetail {
        private Integer home;
        private Integer away;
    }
}

