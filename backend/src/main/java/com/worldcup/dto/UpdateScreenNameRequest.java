package com.worldcup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateScreenNameRequest {
    @NotBlank(message = "Screen name is required")
    @Size(min = 2, max = 50, message = "Screen name must be between 2 and 50 characters")
    private String screenName;
}


