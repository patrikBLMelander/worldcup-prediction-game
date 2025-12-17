package com.worldcup.config;

import com.worldcup.dto.LeaderboardEntryDTO;
import com.worldcup.entity.League;
import com.worldcup.repository.LeagueRepository;
import com.worldcup.service.AchievementService;
import com.worldcup.service.LeagueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler that checks for finished leagues and awards leaderboard achievements
 * Runs daily to check if any leagues have finished and award achievements
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeagueAchievementScheduler {

    private final LeagueRepository leagueRepository;
    private final LeagueService leagueService;
    private final AchievementService achievementService;

    /**
     * Runs daily at 2 AM to check for finished leagues and award achievements
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void checkFinishedLeagues() {
        log.info("Checking for finished leagues to award achievements...");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find all leagues that have finished (endDate <= now) and haven't been processed
        List<League> finishedLeagues = leagueRepository.findFinishedLeagues(now).stream()
            .filter(league -> {
                Boolean processed = league.getAchievementsProcessed();
                return processed == null || !processed; // Treat null as false (not processed)
            })
            .toList();
        
        if (finishedLeagues.isEmpty()) {
            log.debug("No new finished leagues found");
            return;
        }
        
        log.info("Found {} finished league(s) to process", finishedLeagues.size());
        
        // Process each league in its own transaction to avoid long-running transactions
        for (League league : finishedLeagues) {
            processLeague(league);
        }
    }
    
    /**
     * Process a single league in its own transaction
     */
    @Transactional
    private void processLeague(League league) {
        try {
            // Get league leaderboard
            List<LeaderboardEntryDTO> leaderboard = leagueService.getLeagueLeaderboard(league.getId());
            
            // Check and award achievements based on league leaderboard positions
            achievementService.checkLeagueLeaderboardAchievements(league, leaderboard);
            
            // Mark as processed in database
            league.setAchievementsProcessed(true);
            leagueRepository.save(league);
            
            log.info("Processed finished league: {} ({} members)", league.getName(), leaderboard.size());
        } catch (Exception e) {
            log.error("Error processing finished league {}: {}", league.getId(), e.getMessage(), e);
            // Don't rethrow - continue processing other leagues
        }
    }
    
    /**
     * Manual trigger method for testing or immediate processing
     */
    @Transactional
    public void processFinishedLeaguesNow() {
        checkFinishedLeagues();
    }
}

