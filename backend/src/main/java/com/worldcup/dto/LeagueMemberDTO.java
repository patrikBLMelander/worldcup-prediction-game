package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeagueMemberDTO {
    private Long userId;
    private String email;
    private String screenName;
    private String role; // "OWNER" or "MEMBER"
    private LocalDateTime joinedAt;
}

