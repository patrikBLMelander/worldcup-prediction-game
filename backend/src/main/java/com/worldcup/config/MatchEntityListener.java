package com.worldcup.config;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.service.PredictionService;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Entity listener to automatically calculate points when a match is finished.
 * This works even when the database is updated directly (not via API).
 */
@Component
@Slf4j
public class MatchEntityListener implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        MatchEntityListener.applicationContext = applicationContext;
    }

    @PostUpdate
    public void onMatchUpdate(Match match) {
        // Only calculate points if match is finished and has scores
        if (match.getStatus() == MatchStatus.FINISHED 
            && match.getHomeScore() != null 
            && match.getAwayScore() != null) {
            
            try {
                if (applicationContext != null) {
                    PredictionService predictionService = applicationContext.getBean(PredictionService.class);
                    predictionService.calculatePointsForMatch(match.getId());
                    log.info("Automatically calculated points for finished match {}", match.getId());
                }
            } catch (Exception e) {
                log.error("Failed to calculate points for match {}: {}", match.getId(), e.getMessage());
            }
        }
    }
}

