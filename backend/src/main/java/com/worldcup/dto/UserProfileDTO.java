package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String email;
    private String screenName;
    private String role;
    private Integer totalPoints;
    private Integer predictionCount;
    private LocalDateTime createdAt;
}

