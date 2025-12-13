package com.worldcup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileDTO {
    private Long userId;
    private String screenName;
    private Integer totalPoints;
    private Integer predictionCount;
    private PredictionStatisticsDTO statistics;
    private List<FinishedPredictionDTO> finishedPredictions;
}


