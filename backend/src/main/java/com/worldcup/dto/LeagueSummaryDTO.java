package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LeagueSummaryDTO {

    private Long id;
    private String name;
    private String joinCode;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime lockedAt;
    private Long ownerId;
    private String ownerScreenName;
}


