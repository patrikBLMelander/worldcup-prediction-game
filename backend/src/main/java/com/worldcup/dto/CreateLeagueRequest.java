package com.worldcup.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateLeagueRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    /**
     * Start of the scoring window (inclusive).
     * Only matches with kick-off >= startDate are counted.
     */
    @NotNull
    private LocalDateTime startDate;

    /**
     * End of the scoring window (inclusive).
     * Only matches with kick-off <= endDate are counted.
     */
    @NotNull
    @Future
    private LocalDateTime endDate;
}


