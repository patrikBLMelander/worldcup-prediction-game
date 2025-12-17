package com.worldcup.config;

import com.worldcup.service.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeder for achievements - initializes default achievements in the database
 */
@Component
@Order(0) // Run before other seeders
@RequiredArgsConstructor
@Slf4j
public class AchievementSeeder implements CommandLineRunner {

    private final AchievementService achievementService;

    @Override
    public void run(String... args) {
        log.info("Initializing default achievements...");
        achievementService.initializeDefaultAchievements();
        log.info("Default achievements initialized");
    }
}

