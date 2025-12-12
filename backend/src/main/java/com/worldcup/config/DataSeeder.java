package com.worldcup.config;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Data seeder for World Cup 2026 matches
 * Only runs when no matches exist in the database
 */
@Component
@Order(1) // Run before PremierLeagueSeeder
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final MatchRepository matchRepository;

    @Override
    public void run(String... args) {
        if (matchRepository.count() > 0) {
            log.info("Matches already exist, skipping data seeding");
            return;
        }

        log.info("Seeding World Cup 2026 match data...");

        // World Cup 2026 is scheduled for June-July 2026
        // Using dates in 2026 for the tournament
        LocalDateTime baseDate = LocalDateTime.of(2026, 6, 11, 12, 0);

        List<Match> matches = Arrays.asList(
            // Group A matches
            createMatch("Qatar", "Ecuador", baseDate.plusDays(0), "Lusail Stadium", "Group A"),
            createMatch("Senegal", "Netherlands", baseDate.plusDays(0).plusHours(3), "Al Bayt Stadium", "Group A"),
            createMatch("Qatar", "Senegal", baseDate.plusDays(4), "Al Thumama Stadium", "Group A"),
            createMatch("Netherlands", "Ecuador", baseDate.plusDays(4).plusHours(3), "Khalifa International Stadium", "Group A"),
            createMatch("Ecuador", "Senegal", baseDate.plusDays(8), "Al Janoub Stadium", "Group A"),
            createMatch("Netherlands", "Qatar", baseDate.plusDays(8).plusHours(3), "Al Bayt Stadium", "Group A"),

            // Group B matches
            createMatch("England", "Iran", baseDate.plusDays(1), "Khalifa International Stadium", "Group B"),
            createMatch("USA", "Wales", baseDate.plusDays(1).plusHours(3), "Ahmad Bin Ali Stadium", "Group B"),
            createMatch("Wales", "Iran", baseDate.plusDays(5), "Ahmad Bin Ali Stadium", "Group B"),
            createMatch("England", "USA", baseDate.plusDays(5).plusHours(3), "Al Bayt Stadium", "Group B"),
            createMatch("Wales", "England", baseDate.plusDays(9), "Ahmad Bin Ali Stadium", "Group B"),
            createMatch("Iran", "USA", baseDate.plusDays(9).plusHours(3), "Al Thumama Stadium", "Group B"),

            // Group C matches
            createMatch("Argentina", "Saudi Arabia", baseDate.plusDays(1).plusHours(6), "Lusail Stadium", "Group C"),
            createMatch("Mexico", "Poland", baseDate.plusDays(1).plusHours(9), "Stadium 974", "Group C"),
            createMatch("Poland", "Saudi Arabia", baseDate.plusDays(5).plusHours(6), "Education City Stadium", "Group C"),
            createMatch("Argentina", "Mexico", baseDate.plusDays(5).plusHours(9), "Lusail Stadium", "Group C"),
            createMatch("Poland", "Argentina", baseDate.plusDays(9).plusHours(6), "Stadium 974", "Group C"),
            createMatch("Saudi Arabia", "Mexico", baseDate.plusDays(9).plusHours(9), "Lusail Stadium", "Group C"),

            // Group D matches
            createMatch("France", "Australia", baseDate.plusDays(2), "Al Janoub Stadium", "Group D"),
            createMatch("Denmark", "Tunisia", baseDate.plusDays(2).plusHours(3), "Education City Stadium", "Group D"),
            createMatch("Tunisia", "Australia", baseDate.plusDays(6), "Al Janoub Stadium", "Group D"),
            createMatch("France", "Denmark", baseDate.plusDays(6).plusHours(3), "Stadium 974", "Group D"),
            createMatch("Australia", "Denmark", baseDate.plusDays(10), "Al Janoub Stadium", "Group D"),
            createMatch("Tunisia", "France", baseDate.plusDays(10).plusHours(3), "Education City Stadium", "Group D"),

            // Group E matches
            createMatch("Spain", "Costa Rica", baseDate.plusDays(2).plusHours(6), "Al Thumama Stadium", "Group E"),
            createMatch("Germany", "Japan", baseDate.plusDays(2).plusHours(9), "Khalifa International Stadium", "Group E"),
            createMatch("Japan", "Costa Rica", baseDate.plusDays(6).plusHours(6), "Ahmad Bin Ali Stadium", "Group E"),
            createMatch("Spain", "Germany", baseDate.plusDays(6).plusHours(9), "Al Bayt Stadium", "Group E"),
            createMatch("Japan", "Spain", baseDate.plusDays(10).plusHours(6), "Khalifa International Stadium", "Group E"),
            createMatch("Costa Rica", "Germany", baseDate.plusDays(10).plusHours(9), "Al Bayt Stadium", "Group E"),

            // Group F matches
            createMatch("Belgium", "Canada", baseDate.plusDays(3), "Ahmad Bin Ali Stadium", "Group F"),
            createMatch("Morocco", "Croatia", baseDate.plusDays(3).plusHours(3), "Al Bayt Stadium", "Group F"),
            createMatch("Belgium", "Morocco", baseDate.plusDays(7), "Al Thumama Stadium", "Group F"),
            createMatch("Croatia", "Canada", baseDate.plusDays(7).plusHours(3), "Khalifa International Stadium", "Group F"),
            createMatch("Croatia", "Belgium", baseDate.plusDays(11), "Ahmad Bin Ali Stadium", "Group F"),
            createMatch("Canada", "Morocco", baseDate.plusDays(11).plusHours(3), "Al Thumama Stadium", "Group F"),

            // Group G matches
            createMatch("Brazil", "Serbia", baseDate.plusDays(3).plusHours(6), "Lusail Stadium", "Group G"),
            createMatch("Switzerland", "Cameroon", baseDate.plusDays(3).plusHours(9), "Al Janoub Stadium", "Group G"),
            createMatch("Cameroon", "Serbia", baseDate.plusDays(7).plusHours(6), "Al Janoub Stadium", "Group G"),
            createMatch("Brazil", "Switzerland", baseDate.plusDays(7).plusHours(9), "Stadium 974", "Group G"),
            createMatch("Serbia", "Switzerland", baseDate.plusDays(11).plusHours(6), "Stadium 974", "Group G"),
            createMatch("Cameroon", "Brazil", baseDate.plusDays(11).plusHours(9), "Lusail Stadium", "Group G"),

            // Group H matches
            createMatch("Portugal", "Ghana", baseDate.plusDays(4).plusHours(6), "Stadium 974", "Group H"),
            createMatch("Uruguay", "South Korea", baseDate.plusDays(4).plusHours(9), "Education City Stadium", "Group H"),
            createMatch("South Korea", "Ghana", baseDate.plusDays(8).plusHours(6), "Education City Stadium", "Group H"),
            createMatch("Portugal", "Uruguay", baseDate.plusDays(8).plusHours(9), "Lusail Stadium", "Group H"),
            createMatch("Ghana", "Uruguay", baseDate.plusDays(12).plusHours(6), "Al Janoub Stadium", "Group H"),
            createMatch("South Korea", "Portugal", baseDate.plusDays(12).plusHours(9), "Education City Stadium", "Group H"),

            // Round of 16 (sample matches - dates would be after group stage)
            createMatch("Group A Winner", "Group B Runner-up", baseDate.plusDays(14), "Khalifa International Stadium", "Round of 16"),
            createMatch("Group C Winner", "Group D Runner-up", baseDate.plusDays(14).plusHours(3), "Ahmad Bin Ali Stadium", "Round of 16"),
            createMatch("Group E Winner", "Group F Runner-up", baseDate.plusDays(15), "Al Bayt Stadium", "Round of 16"),
            createMatch("Group G Winner", "Group H Runner-up", baseDate.plusDays(15).plusHours(3), "Stadium 974", "Round of 16"),

            // Quarter-Finals
            createMatch("QF1 Winner", "QF2 Winner", baseDate.plusDays(19), "Lusail Stadium", "Quarter-Final"),
            createMatch("QF3 Winner", "QF4 Winner", baseDate.plusDays(19).plusHours(3), "Al Bayt Stadium", "Quarter-Final"),

            // Semi-Finals
            createMatch("SF1 Winner", "SF2 Winner", baseDate.plusDays(23), "Lusail Stadium", "Semi-Final"),

            // Final
            createMatch("Finalist 1", "Finalist 2", baseDate.plusDays(26), "Lusail Stadium", "Final")
        );

        matchRepository.saveAll(matches);
        log.info("Successfully seeded {} matches", matches.size());
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

