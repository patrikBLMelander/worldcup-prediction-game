package com.worldcup.config;

import com.worldcup.entity.Prediction;
import com.worldcup.entity.PredictionOutcome;
import com.worldcup.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time backfill for the switch to Copabet-style outcome scoring.
 *
 * <p>Legacy predictions stored exact scores but no {@link PredictionOutcome}.
 * This derives the outcome from those scores so historical predictions keep
 * counting. Runs once at startup and is a no-op afterwards.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PredictionOutcomeBackfill implements ApplicationRunner {

    private final PredictionRepository predictionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Prediction> all = predictionRepository.findAll();
        int updated = 0;

        for (Prediction p : all) {
            if (p.getPredictedOutcome() != null) continue;
            if (p.getPredictedHomeScore() == null || p.getPredictedAwayScore() == null) continue;

            p.setPredictedOutcome(
                PredictionOutcome.fromScores(p.getPredictedHomeScore(), p.getPredictedAwayScore()));
            predictionRepository.save(p);
            updated++;
        }

        if (updated > 0) {
            log.info("Backfilled predictedOutcome for {} legacy prediction(s)", updated);
        }
    }
}
