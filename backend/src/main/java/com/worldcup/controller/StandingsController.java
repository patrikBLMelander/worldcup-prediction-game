package com.worldcup.controller;

import com.worldcup.service.FootballApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
public class StandingsController {

    private final FootballApiService footballApiService;

    @GetMapping
    public ResponseEntity<List<FootballApiService.StandingData>> getStandings() {
        return ResponseEntity.ok(footballApiService.fetchStandings());
    }
}
