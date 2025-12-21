package com.worldcup.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when a date range is invalid (e.g., end date before start date).
 */
public class InvalidDateRangeException extends WorldCupException {
    
    public InvalidDateRangeException(LocalDateTime startDate, LocalDateTime endDate) {
        super("INVALID_DATE_RANGE", String.format("End date (%s) must be after start date (%s).", endDate, startDate));
    }
    
    public InvalidDateRangeException(String message) {
        super("INVALID_DATE_RANGE", message);
    }
}

