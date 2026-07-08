package com.worldcup.controller;

import com.worldcup.dto.CreateLeagueRequest;
import com.worldcup.dto.JoinLeagueRequest;
import com.worldcup.dto.LeagueSummaryDTO;
import com.worldcup.dto.LeagueMemberDTO;
import com.worldcup.dto.LeaderboardEntryDTO;
import com.worldcup.dto.LeaguePredictionSplitDTO;
import com.worldcup.dto.LeagueScoreTimelineDTO;
import com.worldcup.entity.MatchStatus;
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

    /**
     * How this league's members predicted a single locked match. 403 if the caller
     * isn't a member; 400 if the match hasn't kicked off yet.
     */
    @GetMapping("/{leagueId}/match-predictions/{matchId}")
    public ResponseEntity<LeaguePredictionSplitDTO> getMatchPredictionSplit(
            @PathVariable Long leagueId, @PathVariable Long matchId) {
        User user = currentUser.getCurrentUserOrThrow();
        return ResponseEntity.ok(leagueService.getMatchPredictionSplit(leagueId, matchId, user));
    }

    /**
     * How this league's members predicted every locked match in the league window
     * (newest first). Powers the league "Predictions" tab. Pass {@code ?status=LIVE}
     * to get only in-progress matches (used by the leaderboard's live section).
     */
    @GetMapping("/{leagueId}/match-predictions")
    public ResponseEntity<List<LeaguePredictionSplitDTO>> getMatchPredictionSplits(
            @PathVariable Long leagueId,
            @RequestParam(required = false) MatchStatus status) {
        User user = currentUser.getCurrentUserOrThrow();
        return ResponseEntity.ok(leagueService.getLockedMatchPredictionSplits(leagueId, user, status));
    }

    /**
     * Cumulative points over time for every league member (one point per scored
     * match). Powers the leaderboard's "Over time" line chart.
     */
    @GetMapping("/{leagueId}/score-timeline")
    public ResponseEntity<LeagueScoreTimelineDTO> getScoreTimeline(@PathVariable Long leagueId) {
        User user = currentUser.getCurrentUserOrThrow();
        return ResponseEntity.ok(leagueService.getScoreTimeline(leagueId, user));
    }

    @PostMapping("/{leagueId}/hide")
    public ResponseEntity<?> hideLeague(@PathVariable Long leagueId) {
        User user = currentUser.getCurrentUserOrThrow();
        leagueService.hideLeague(leagueId, user);
        return ResponseEntity.ok().body(java.util.Map.of("message", "League hidden successfully"));
    }

    @PostMapping("/{leagueId}/unhide")
    public ResponseEntity<?> unhideLeague(@PathVariable Long leagueId) {
        User user = currentUser.getCurrentUserOrThrow();
        leagueService.unhideLeague(leagueId, user);
        return ResponseEntity.ok().body(java.util.Map.of("message", "League unhidden successfully"));
    }
}


