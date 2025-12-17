package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String category;
    private Integer rarity;
    private Boolean earned;
    private String earnedAt; // ISO date string or null if not earned
}

