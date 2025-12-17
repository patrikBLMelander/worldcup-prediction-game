package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievementDTO {
    private Long id;
    private AchievementDTO achievement;
    private LocalDateTime earnedAt;
}

