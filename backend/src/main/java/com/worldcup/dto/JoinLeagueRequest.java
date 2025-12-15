package com.worldcup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JoinLeagueRequest {

    @NotBlank
    @Size(max = 32)
    private String joinCode;
}


