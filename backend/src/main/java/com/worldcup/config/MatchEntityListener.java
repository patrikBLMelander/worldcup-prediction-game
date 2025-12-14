package com.worldcup.config;

import com.worldcup.entity.Match;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Entity listener for match updates.
 * Point calculation is now handled manually in controllers after transaction commits
 * to avoid transaction conflicts.
 */
@Component
@Slf4j
public class MatchEntityListener {

    @PostUpdate
    public void onMatchUpdate(Match match) {
        // Temporarily disabled to avoid transaction conflicts
        // Point calculation is now handled manually in controllers after transaction commits
        // This prevents 500 errors when updating match results from admin panel
        log.debug("Match {} updated to status {} - point calculation handled separately", 
            match.getId(), match.getStatus());
    }
}

