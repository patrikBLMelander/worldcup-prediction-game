package com.worldcup.config;

import com.worldcup.entity.Prediction;
import com.worldcup.entity.PredictionOutcome;
import com.worldcup.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-time migration for the switch to Copabet-style outcome scoring.
 *
 * <p>Two jobs, both idempotent and safe to run on every startup:
 * <ol>
 *   <li>Relax the legacy {@code predicted_home_score} / {@code predicted_away_score}
 *       columns so they accept NULL. The columns predate outcome scoring and were
 *       created NOT NULL; {@code ddl-auto=update} never relaxes that, so new
 *       outcome-only predictions (scores NULL) would fail to insert.</li>
 *   <li>Derive {@link PredictionOutcome} from the scores of any legacy rows that
 *       don't have one yet, so historical predictions keep counting.</li>
 * </ol>
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PredictionOutcomeBackfill implements ApplicationRunner {

    private final PredictionRepository predictionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        relaxLegacyScoreColumns();
        backfillOutcomes();
    }

    /**
     * Drop NOT NULL on the legacy score columns. Standard SQL supported by both
     * Postgres and H2 2.x; dropping an already-nullable constraint is a no-op.
     */
    private void relaxLegacyScoreColumns() {
        dropNotNull("predicted_home_score");
        dropNotNull("predicted_away_score");
    }

    private void dropNotNull(String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE predictions ALTER COLUMN " + column + " DROP NOT NULL");
        } catch (Exception e) {
            // Already nullable, table not created yet, or dialect quirk - safe to ignore.
            log.debug("Could not drop NOT NULL on predictions.{} (likely already nullable): {}",
                    column, e.getMessage());
        }
    }

    private void backfillOutcomes() {
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
