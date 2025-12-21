package com.worldcup.controller;

import com.worldcup.dto.CreateLeagueRequest;
import com.worldcup.dto.JoinLeagueRequest;
import com.worldcup.dto.LeagueSummaryDTO;
import com.worldcup.dto.LeagueMemberDTO;
import com.worldcup.dto.LeaderboardEntryDTO;
import com.worldcup.entity.User;
import com.worldcup.security.CurrentUser;
import com.worldcup.service.LeagueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<LeagueSummaryDTO> createLeague(@Valid @RequestBody CreateLeagueRequest request) {
        User user = currentUser.getCurrentUserOrThrow();
        LeagueSummaryDTO league = leagueService.createLeague(request, user);
        return ResponseEntity.ok(league);
    }

    @PostMapping("/join")
    public ResponseEntity<LeagueSummaryDTO> joinLeague(@Valid @RequestBody JoinLeagueRequest request) {
        User user = currentUser.getCurrentUserOrThrow();
        LeagueSummaryDTO league = leagueService.joinLeagueByCode(request.getJoinCode(), user);
        return ResponseEntity.ok(league);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<LeagueSummaryDTO>> getMyLeagues() {
        User user = currentUser.getCurrentUserOrThrow();
        List<LeagueSummaryDTO> leagues = leagueService.getLeaguesForUser(user);
        return ResponseEntity.ok(leagues);
    }

    @GetMapping("/{leagueId}/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeagueLeaderboard(@PathVariable Long leagueId) {
        // No need for current user here; any authenticated user can view a league they belong to.
        // Access control by membership can be added later if desired.
        List<LeaderboardEntryDTO> leaderboard = leagueService.getLeagueLeaderboard(leagueId);
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/{leagueId}/members")
    public ResponseEntity<List<LeagueMemberDTO>> getLeagueMembers(@PathVariable Long leagueId) {
        List<LeagueMemberDTO> members = leagueService.getLeagueMembers(leagueId);
        return ResponseEntity.ok(members);
    }

    // TODO: League deletion feature - temporarily disabled for production safety
    // Will be enabled after fixing notification cleanup pattern mismatch
    /*
    @DeleteMapping("/{leagueId}")
    public ResponseEntity<?> deleteLeague(@PathVariable Long leagueId) {
        User user = currentUser.getCurrentUserOrThrow();
        leagueService.deleteLeague(leagueId, user);
        return ResponseEntity.ok().body(java.util.Map.of("message", "League deleted successfully"));
    }
    */
}


