package com.worldcup.service;

import com.worldcup.dto.CreateLeagueRequest;
import com.worldcup.dto.LeagueSummaryDTO;
import com.worldcup.dto.LeagueMemberDTO;
import com.worldcup.dto.LeaderboardEntryDTO;
import com.worldcup.entity.League;
import com.worldcup.entity.LeagueMembership;
import com.worldcup.entity.LeagueRole;
import com.worldcup.entity.User;
import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.entity.Prediction;
import com.worldcup.repository.LeagueMembershipRepository;
import com.worldcup.repository.LeagueRepository;
import com.worldcup.repository.PredictionRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final PredictionRepository predictionRepository;
    private final PredictionService predictionService;

    public LeagueSummaryDTO createLeague(CreateLeagueRequest request, User owner) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // For now we assume validation of being within competition window is handled elsewhere or not needed

        League league = new League();
        league.setName(request.getName().trim());
        league.setOwner(owner);
        league.setStartDate(request.getStartDate());
        league.setEndDate(request.getEndDate());
        league.setJoinCode(generateJoinCode());

        League savedLeague = leagueRepository.save(league);

        // Owner is automatically a member with OWNER role
        LeagueMembership membership = new LeagueMembership();
        membership.setLeague(savedLeague);
        membership.setUser(owner);
        membership.setRole(LeagueRole.OWNER);
        membershipRepository.save(membership);

        return toSummary(savedLeague);
    }

    public LeagueSummaryDTO joinLeagueByCode(String joinCode, User user) {
        League league = leagueRepository.findByJoinCode(joinCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("League not found for provided code"));

        LocalDateTime now = LocalDateTime.now();
        // Disallow joining after league window has started (join deadline)
        if (!now.isBefore(league.getStartDate())) {
            throw new IllegalStateException("League is locked for new members");
        }

        // Ensure user is not already a member
        Optional<LeagueMembership> existing = membershipRepository.findByLeagueAndUser(league, user);
        if (existing.isPresent()) {
            return toSummary(league); // Idempotent join
        }

        LeagueMembership membership = new LeagueMembership();
        membership.setLeague(league);
        membership.setUser(user);
        membership.setRole(LeagueRole.MEMBER);
        membershipRepository.save(membership);

        return toSummary(league);
    }

    @Transactional(readOnly = true)
    public List<LeagueSummaryDTO> getLeaguesForUser(User user) {
        List<LeagueMembership> memberships = membershipRepository.findByUser(user);
        return memberships.stream()
                .map(LeagueMembership::getLeague)
                .distinct()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private String generateJoinCode() {
        // Simple random uppercase code; uniqueness is enforced by DB constraint
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeagueLeaderboard(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new IllegalArgumentException("League not found"));

        LocalDateTime start = league.getStartDate();
        LocalDateTime end = league.getEndDate();

        List<LeagueMembership> memberships = membershipRepository.findByLeague(league);

        return memberships.stream()
                .map(LeagueMembership::getUser)
                .distinct()
                .map(user -> buildLeagueEntryForUser(user, start, end))
                .sorted((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()))
                .collect(Collectors.toList());
    }

    private LeaderboardEntryDTO buildLeagueEntryForUser(User user, LocalDateTime start, LocalDateTime end) {
        List<Prediction> predictions;
        try {
            predictions = predictionRepository.findByUserWithMatch(user);
        } catch (Exception e) {
            log.warn("JOIN FETCH query failed for user {}, falling back to regular query: {}", user.getId(), e.getMessage());
            predictions = predictionRepository.findByUser(user);
        }

        int totalPoints = 0;
        int predictionCount = 0;

        for (Prediction p : predictions) {
            Match match = p.getMatch();
            if (match == null) continue;

            LocalDateTime kickOff = match.getMatchDate();
            if (kickOff.isBefore(start) || kickOff.isAfter(end)) continue;

            // Only count LIVE/FINISHED matches with scores
            MatchStatus status = match.getStatus();
            if (status != MatchStatus.FINISHED && status != MatchStatus.LIVE) continue;
            if (match.getHomeScore() == null || match.getAwayScore() == null) continue;

            // Ensure points exist - only calculate and save for FINISHED matches
            Integer points = p.getPoints();
            if (points == null &&
                p.getPredictedHomeScore() != null &&
                p.getPredictedAwayScore() != null) {
                points = predictionService.calculatePointsForPrediction(
                        p.getPredictedHomeScore(),
                        p.getPredictedAwayScore(),
                        match.getHomeScore(),
                        match.getAwayScore()
                );
                
                // Only save points for FINISHED matches
                if (status == MatchStatus.FINISHED) {
                    p.setPoints(points);
                    predictionRepository.save(p);
                }
                // For LIVE matches, use points for display only (don't save)
            }

            if (points != null) {
                totalPoints += points;
                predictionCount++;
            }
        }

        return new LeaderboardEntryDTO(
                user.getId(),
                user.getEmail(),
                user.getScreenName(),
                totalPoints,
                predictionCount
        );
    }

    @Transactional(readOnly = true)
    public List<LeagueMemberDTO> getLeagueMembers(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new IllegalArgumentException("League not found"));

        List<LeagueMembership> memberships = membershipRepository.findByLeague(league);
        
        return memberships.stream()
                .map(membership -> {
                    User user = membership.getUser();
                    return new LeagueMemberDTO(
                            user.getId(),
                            user.getEmail(),
                            user.getScreenName(),
                            membership.getRole().name(),
                            membership.getJoinedAt()
                    );
                })
                .sorted((a, b) -> {
                    // Sort: OWNER first, then by joined date
                    if (a.getRole().equals("OWNER") && !b.getRole().equals("OWNER")) {
                        return -1;
                    }
                    if (!a.getRole().equals("OWNER") && b.getRole().equals("OWNER")) {
                        return 1;
                    }
                    return a.getJoinedAt().compareTo(b.getJoinedAt());
                })
                .collect(Collectors.toList());
    }

    private LeagueSummaryDTO toSummary(League league) {
        User owner = league.getOwner();
        return new LeagueSummaryDTO(
                league.getId(),
                league.getName(),
                league.getJoinCode(),
                league.getStartDate(),
                league.getEndDate(),
                league.getLockedAt(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getScreenName() : null
        );
    }
}


