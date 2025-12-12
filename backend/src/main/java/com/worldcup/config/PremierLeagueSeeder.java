package com.worldcup.config;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.repository.MatchRepository;
import com.worldcup.service.FootballApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data seeder for Premier League matches during Christmas period
 * Dec 19, 2025 - Jan 6, 2026
 */
@Component
@Order(2) // Run after the main DataSeeder
@RequiredArgsConstructor
@Slf4j
public class PremierLeagueSeeder implements CommandLineRunner {

    private final MatchRepository matchRepository;
    private final FootballApiService footballApiService;

    @Value("${football.api.enabled:false}")
    private boolean apiEnabled;

    // Premier League teams
    private static final String[] PREMIER_LEAGUE_TEAMS = {
        "Arsenal", "Aston Villa", "Bournemouth", "Brentford", "Brighton",
        "Burnley", "Chelsea", "Crystal Palace", "Everton", "Fulham",
        "Liverpool", "Luton Town", "Manchester City", "Manchester United",
        "Newcastle United", "Nottingham Forest", "Sheffield United", "Tottenham",
        "West Ham", "Wolves"
    };

    // Premier League stadiums
    private static final String[] STADIUMS = {
        "Emirates Stadium", "Villa Park", "Vitality Stadium", "Gtech Community Stadium",
        "American Express Community Stadium", "Turf Moor", "Stamford Bridge",
        "Selhurst Park", "Goodison Park", "Craven Cottage", "Anfield",
        "Kenilworth Road", "Etihad Stadium", "Old Trafford", "St. James' Park",
        "City Ground", "Bramall Lane", "Tottenham Hotspur Stadium",
        "London Stadium", "Molineux Stadium"
    };

    @Override
    public void run(String... args) {
        // Check if Premier League matches already exist
        long premierLeagueCount = matchRepository.findAll().stream()
            .filter(m -> m.getGroup() != null && m.getGroup().equals("Premier League"))
            .count();

        if (premierLeagueCount > 0) {
            log.info("Premier League matches already exist ({} matches), skipping seeding", premierLeagueCount);
            return;
        }

        // If API is enabled, fetch real fixtures
        if (apiEnabled) {
            log.info("Football API is enabled - fetching real Premier League fixtures for Dec 19, 2025 - Jan 6, 2026...");
            fetchRealFixtures();
            return;
        }

        log.info("Football API is disabled - using mock data for Premier League matches (Dec 19, 2025 - Jan 6, 2026)");
        log.warn("To use real fixtures, enable the Football API in application.properties");
        generateMockFixtures();
    }

    private void fetchRealFixtures() {
        try {
            LocalDateTime startDate = LocalDateTime.of(2025, 12, 19, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2026, 1, 6, 23, 59);

            List<FootballApiService.MatchData> apiMatches = footballApiService.fetchMatches(startDate, endDate);

            if (apiMatches.isEmpty()) {
                log.warn("No matches found from API for the specified date range. Falling back to mock data.");
                generateMockFixtures();
                return;
            }

            List<Match> matches = new ArrayList<>();
            for (FootballApiService.MatchData apiMatch : apiMatches) {
                Match match = footballApiService.convertToMatch(apiMatch);
                matches.add(match);
            }

            if (!matches.isEmpty()) {
                matchRepository.saveAll(matches);
                log.info("Successfully seeded {} real Premier League matches from API", matches.size());
            } else {
                log.warn("No matches were created from API. Falling back to mock data.");
                generateMockFixtures();
            }
        } catch (Exception e) {
            log.error("Error fetching real fixtures from API: {}", e.getMessage(), e);
            log.warn("Falling back to mock data generation");
            generateMockFixtures();
        }
    }

    private void generateMockFixtures() {

        List<Match> matches = new ArrayList<>();
        LocalDateTime startDate = LocalDateTime.of(2025, 12, 19, 12, 0); // Dec 19, 2025, 12:00 PM UTC
        LocalDateTime endDate = LocalDateTime.of(2026, 1, 6, 22, 0); // Jan 6, 2026, 10:00 PM UTC

        // Generate matches for the period
        LocalDateTime currentDate = startDate;
        int matchNumber = 0;
        java.util.Set<String> usedFixtures = new java.util.HashSet<>(); // Track fixtures to avoid duplicates

        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            // Skip Christmas Day (Dec 25) - no matches traditionally
            if (currentDate.getDayOfMonth() == 25 && currentDate.getMonthValue() == 12) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Generate 5-10 matches per day (typical Premier League matchday)
            int matchesPerDay = 5 + (int)(Math.random() * 6); // 5-10 matches
            
            for (int i = 0; i < matchesPerDay && matchNumber < 150; i++) { // Limit to 150 matches
                String homeTeam = PREMIER_LEAGUE_TEAMS[(int)(Math.random() * PREMIER_LEAGUE_TEAMS.length)];
                String awayTeam;
                String fixtureKey;
                int attempts = 0;
                
                // Ensure different teams and avoid duplicate fixtures
                do {
                    awayTeam = PREMIER_LEAGUE_TEAMS[(int)(Math.random() * PREMIER_LEAGUE_TEAMS.length)];
                    fixtureKey = homeTeam + " vs " + awayTeam;
                    attempts++;
                } while ((awayTeam.equals(homeTeam) || usedFixtures.contains(fixtureKey)) && attempts < 50);
                
                if (awayTeam.equals(homeTeam)) {
                    continue; // Skip if couldn't find different team
                }
                
                usedFixtures.add(fixtureKey);

                // Match times: 12:00, 15:00, 17:30, 20:00 (UTC)
                int[] matchTimes = {12, 15, 17, 20};
                int hour = matchTimes[i % matchTimes.length];
                int minute = (i % 2 == 0 && hour != 17) ? 0 : (hour == 17 ? 30 : 0);

                LocalDateTime matchDateTime = currentDate.withHour(hour).withMinute(minute);
                // Use home team's stadium (simplified - just use a random stadium for now)
                String venue = STADIUMS[(int)(Math.random() * STADIUMS.length)];

                Match match = createMatch(homeTeam, awayTeam, matchDateTime, venue, "Premier League");
                matches.add(match);
                matchNumber++;
            }

            currentDate = currentDate.plusDays(1);
        }

        if (!matches.isEmpty()) {
            matchRepository.saveAll(matches);
            log.info("Successfully seeded {} Premier League matches", matches.size());
        } else {
            log.warn("No Premier League matches were created");
        }
    }

    private Match createMatch(String homeTeam, String awayTeam, LocalDateTime matchDate, 
                             String venue, String group) {
        Match match = new Match();
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setMatchDate(matchDate);
        match.setVenue(venue);
        match.setGroup(group);
        match.setStatus(MatchStatus.SCHEDULED);
        return match;
    }
}

