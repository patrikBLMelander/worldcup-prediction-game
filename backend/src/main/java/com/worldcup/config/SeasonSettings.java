package com.worldcup.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Start of the current season.
 *
 * The global leaderboard is an all-time sum of every prediction's points, so
 * without a cut-off the previous tournament's scores follow us into the next
 * one. Setting {@code app.season.start} (env: {@code APP_SEASON_START}) to an
 * ISO date-time scopes the global leaderboard to matches kicking off at or
 * after that instant. Nothing is deleted: older predictions, profile totals
 * and earned achievements are untouched.
 *
 * Leave it empty for all-time behaviour.
 */
@Component
@Slf4j
public class SeasonSettings {

    private final LocalDateTime start;

    public SeasonSettings(@Value("${app.season.start:}") String configuredStart) {
        this.start = parse(configuredStart);
        if (start != null) {
            log.info("Season start set to {} - global leaderboard only counts matches from then on", start);
        }
    }

    /**
     * Empty when no season start is configured, meaning "count everything".
     */
    public Optional<LocalDateTime> getStart() {
        return Optional.ofNullable(start);
    }

    private static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            log.error("Ignoring invalid app.season.start '{}' - expected ISO date-time like 2026-09-01T00:00", value);
            return null;
        }
    }
}
