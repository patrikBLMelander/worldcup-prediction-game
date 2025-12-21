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
import com.worldcup.exception.InvalidBettingConfigurationException;
import com.worldcup.exception.InvalidDateRangeException;
import com.worldcup.exception.LeagueLockedException;
import com.worldcup.exception.LeagueNotFoundException;
import com.worldcup.exception.UnauthorizedException;
import com.worldcup.repository.LeagueMembershipRepository;
import com.worldcup.repository.LeagueRepository;
import com.worldcup.repository.NotificationRepository;
import com.worldcup.repository.PredictionRepository;
import com.worldcup.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final PointsCalculationService pointsCalculationService;
    private final Optional<NotificationService> notificationService; // Optional - may not be available during startup
    private final NotificationRepository notificationRepository;

    public LeagueSummaryDTO createLeague(CreateLeagueRequest request, User owner) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidDateRangeException(request.getStartDate(), request.getEndDate());
        }

        // Validate betting configuration
        if (request.getBettingType() == League.BettingType.FLAT_STAKES) {
            if (request.getEntryPrice() == null || request.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidBettingConfigurationException("Entry price is required for Flat Stakes leagues");
            }
            if (request.getPayoutStructure() == null) {
                // Auto-select: Winner Takes All if < 5 expected players, Ranked if >= 6
                // For now, default to Winner Takes All (can be changed later based on member count)
                request.setPayoutStructure(League.PayoutStructure.WINNER_TAKES_ALL);
            }
            if (request.getPayoutStructure() == League.PayoutStructure.RANKED) {
                if (request.getRankedPercentages() == null || request.getRankedPercentages().isEmpty()) {
                    throw new InvalidBettingConfigurationException("Ranked percentages are required for Ranked payout structure");
                }
                // Validate percentages sum to 1.0 (100%)
                BigDecimal total = request.getRankedPercentages().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (total.compareTo(BigDecimal.ONE) != 0) {
                    throw new InvalidBettingConfigurationException("Ranked percentages must sum to 1.0 (100%)");
                }
            }
        }

        League league = new League();
        league.setName(request.getName().trim());
        league.setOwner(owner);
        league.setStartDate(request.getStartDate());
        league.setEndDate(request.getEndDate());
        league.setJoinCode(generateJoinCode());
        
        // Set betting fields
        league.setBettingType(request.getBettingType());
        league.setEntryPrice(request.getEntryPrice());
        league.setPayoutStructure(request.getPayoutStructure());
        league.setRankedPercentages(request.getRankedPercentages());

        League savedLeague = leagueRepository.save(league);

        // Owner is automatically a member with OWNER role
        LeagueMembership membership = new LeagueMembership();
        membership.setLeague(savedLeague);
        membership.setUser(owner);
        membership.setRole(LeagueRole.OWNER);
        membershipRepository.save(membership);

        return toSummary(savedLeague);
    }

    @Transactional
    public LeagueSummaryDTO joinLeagueByCode(String joinCode, User user) {
        try {
            log.debug("Attempting to join league with code: {}, user: {}", joinCode, user.getId());
            
            League league = leagueRepository.findByJoinCode(joinCode.trim())
                    .orElseThrow(() -> new LeagueNotFoundException(joinCode));

            // Prevent joining hidden leagues
            if (Boolean.TRUE.equals(league.getHidden())) {
                log.warn("User {} attempted to join hidden league {}", user.getId(), league.getId());
                throw new LeagueNotFoundException(joinCode);
            }

            log.debug("Found league: {}, startDate: {}, endDate: {}", league.getId(), league.getStartDate(), league.getEndDate());

            LocalDateTime now = LocalDateTime.now();
            log.debug("Current time: {}, League start date: {}, isBefore: {}", now, league.getStartDate(), now.isBefore(league.getStartDate()));
            
            // Disallow joining after league window has started (join deadline)
            if (!now.isBefore(league.getStartDate())) {
                log.warn("User {} attempted to join league {} which has already started", user.getId(), league.getId());
                throw new LeagueLockedException(league.getId());
            }

            // Ensure user is not already a member
            Optional<LeagueMembership> existing = membershipRepository.findByLeagueAndUser(league, user);
            if (existing.isPresent()) {
                return toSummary(league); // Idempotent join
            }

            // Fetch existing members BEFORE saving new membership (for notifications)
            List<LeagueMembership> existingMembers = membershipRepository.findByLeague(league);

            LeagueMembership membership = new LeagueMembership();
            membership.setLeague(league);
            membership.setUser(user);
            membership.setRole(LeagueRole.MEMBER);
            
            // Handle race condition: if another thread already added this membership, catch the constraint violation
            try {
                membershipRepository.save(membership);
            } catch (DataIntegrityViolationException e) {
                // Another thread already added this membership - that's okay, return idempotently
                log.debug("User {} already a member of league {} (race condition handled)", user.getId(), league.getId());
                return toSummary(league);
            }

        // Notify all existing members (excluding the new member who just joined)
        notificationService.ifPresent(service -> {
            try {
                String newMemberName = user.getScreenName() != null && !user.getScreenName().isEmpty() 
                    ? user.getScreenName() 
                    : user.getEmail();
                String message = String.format("%s joined %s", newMemberName, league.getName());
                
                for (LeagueMembership existingMember : existingMembers) {
                    // Skip notifying the new member themselves
                    if (existingMember.getUser().getId().equals(user.getId())) {
                        continue;
                    }
                    
                    try {
                        service.sendNotification(
                            existingMember.getUser(),
                            Notification.NotificationType.LEAGUE_MEMBER_JOINED,
                            "New Member Joined",
                            message,
                            "👤",
                            "/leagues?league=" + league.getId()
                        );
                    } catch (Exception e) {
                        log.error("Error sending notification to user {} about new league member: {}", 
                                existingMember.getUser().getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error notifying league members about new join: {}", e.getMessage());
                // Don't fail the join operation if notification fails
            }
        });

            return toSummary(league);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Re-throw validation exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error joining league with code {}: {}", joinCode, e.getMessage(), e);
            throw new RuntimeException("Failed to join league: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<LeagueSummaryDTO> getLeaguesForUser(User user) {
        List<LeagueMembership> memberships = membershipRepository.findByUser(user);
        return memberships.stream()
                .map(LeagueMembership::getLeague)
                .filter(league -> !Boolean.TRUE.equals(league.getHidden())) // Filter out hidden leagues
                .distinct()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * Generates a unique join code for a league.
     * Retries up to 10 times if a collision occurs (unlikely but possible with 8-character codes).
     * 
     * @return a unique 8-character uppercase code
     * @throws RuntimeException if unable to generate a unique code after max attempts
     */
    private String generateJoinCode() {
        int maxAttempts = 10;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            
            // Check if code already exists (unlikely but possible)
            if (!leagueRepository.findByJoinCode(code).isPresent()) {
                return code;
            }
            
            log.warn("Join code collision detected on attempt {}: {}", attempt, code);
        }
        
        throw new RuntimeException("Failed to generate unique join code after " + maxAttempts + " attempts");
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeagueLeaderboard(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new LeagueNotFoundException(leagueId));

        // Prevent access to hidden leagues
        if (Boolean.TRUE.equals(league.getHidden())) {
            throw new LeagueNotFoundException(leagueId);
        }

        LocalDateTime start = league.getStartDate();
        LocalDateTime end = league.getEndDate();

        List<LeagueMembership> memberships = membershipRepository.findByLeague(league);

        // Build leaderboard entries
        List<LeaderboardEntryDTO> entries = memberships.stream()
                .map(LeagueMembership::getUser)
                .distinct()
                .map(user -> buildLeagueEntryForUser(user, start, end))
                .sorted((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()))
                .collect(Collectors.toList());

        // Assign ranks and calculate prizes for Flat Stakes leagues
        if (league.getBettingType() == League.BettingType.FLAT_STAKES && league.getEntryPrice() != null) {
            assignRanksAndPrizes(entries, league);
        } else {
            // Just assign ranks without prizes
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setRank(i + 1);
            }
        }

        return entries;
    }

    /**
     * Assign ranks to leaderboard entries, handling ties, and calculate prize amounts for Flat Stakes leagues.
     */
    private void assignRanksAndPrizes(List<LeaderboardEntryDTO> entries, League league) {
        if (entries.isEmpty()) {
            return;
        }

        // Safety check: ensure entryPrice is not null
        if (league.getEntryPrice() == null) {
            log.warn("League {} has FLAT_STAKES but null entryPrice, skipping prize calculation", league.getId());
            // Just assign ranks without prizes
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setRank(i + 1);
            }
            return;
        }

        // Calculate total pot
        int memberCount = entries.size();
        BigDecimal totalPot = league.getEntryPrice().multiply(BigDecimal.valueOf(memberCount));

        // Group entries by points to handle ties
        Map<Integer, List<LeaderboardEntryDTO>> entriesByPoints = entries.stream()
                .collect(Collectors.groupingBy(LeaderboardEntryDTO::getTotalPoints));

        // Sort by points descending
        List<Integer> sortedPoints = entriesByPoints.keySet().stream()
                .sorted((a, b) -> Integer.compare(b, a))
                .collect(Collectors.toList());

        int currentRank = 1;
        List<PrizeTier> prizeTiers = new ArrayList<>();

        // Build prize tiers (handling ties)
        for (Integer points : sortedPoints) {
            List<LeaderboardEntryDTO> tiedEntries = entriesByPoints.get(points);
            int tiedCount = tiedEntries.size();
            
            // Calculate which ranks this tier covers
            int endRank = currentRank + tiedCount - 1;
            
            // Calculate prize for this tier
            BigDecimal tierPrize = calculateTierPrize(league, currentRank, endRank, totalPot, memberCount);
            
            prizeTiers.add(new PrizeTier(currentRank, endRank, tierPrize, tiedEntries));
            
            // Assign ranks to entries
            for (LeaderboardEntryDTO entry : tiedEntries) {
                entry.setRank(currentRank);
            }
            
            currentRank += tiedCount;
        }

        // Distribute prizes
        for (PrizeTier tier : prizeTiers) {
            // Defensive check: skip empty tiers
            if (tier.entries.isEmpty()) {
                log.warn("Empty prize tier detected for league {}, skipping", league.getId());
                continue;
            }
            
            BigDecimal prizePerPlayer = tier.prize.divide(
                BigDecimal.valueOf(tier.entries.size()), 
                2, 
                RoundingMode.HALF_UP
            );
            for (LeaderboardEntryDTO entry : tier.entries) {
                entry.setPrizeAmount(prizePerPlayer);
            }
        }
    }

    /**
     * Calculate prize for a tier (which may span multiple ranks due to ties).
     */
    private BigDecimal calculateTierPrize(League league, int startRank, int endRank, BigDecimal totalPot, int memberCount) {
        if (league.getPayoutStructure() == League.PayoutStructure.WINNER_TAKES_ALL) {
            // Only 1st place gets prize
            if (startRank == 1) {
                return totalPot;
            }
            return BigDecimal.ZERO;
        } else if (league.getPayoutStructure() == League.PayoutStructure.RANKED) {
            // Sum percentages for all ranks in this tier
            BigDecimal totalPercentage = BigDecimal.ZERO;
            Map<Integer, BigDecimal> percentages = league.getRankedPercentages();
            
            if (percentages != null) {
                for (int rank = startRank; rank <= endRank; rank++) {
                    BigDecimal percentage = percentages.get(rank);
                    if (percentage != null) {
                        totalPercentage = totalPercentage.add(percentage);
                    }
                }
            }
            
            return totalPot.multiply(totalPercentage);
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Helper class for prize tier calculation.
     */
    private static class PrizeTier {
        final int startRank;
        final int endRank;
        final BigDecimal prize;
        final List<LeaderboardEntryDTO> entries;

        PrizeTier(int startRank, int endRank, BigDecimal prize, List<LeaderboardEntryDTO> entries) {
            this.startRank = startRank;
            this.endRank = endRank;
            this.prize = prize;
            this.entries = entries;
        }
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
                points = pointsCalculationService.calculatePoints(
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
                predictionCount,
                null, // prizeAmount - will be calculated later
                null  // rank - will be assigned later
        );
    }

    @Transactional(readOnly = true)
    public List<LeagueMemberDTO> getLeagueMembers(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new LeagueNotFoundException(leagueId));

        // Prevent access to hidden leagues
        if (Boolean.TRUE.equals(league.getHidden())) {
            throw new LeagueNotFoundException(leagueId);
        }

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
        try {
            User owner = league.getOwner();
            if (owner == null) {
                log.warn("League {} has no owner", league.getId());
            }
            
            LeagueSummaryDTO dto = new LeagueSummaryDTO(
                    league.getId(),
                    league.getName(),
                    league.getJoinCode(),
                    league.getStartDate(),
                    league.getEndDate(),
                    league.getLockedAt(),
                    owner != null ? owner.getId() : null,
                    owner != null ? owner.getScreenName() : null,
                    league.getBettingType(),
                    league.getEntryPrice(),
                    league.getPayoutStructure(),
                    league.getRankedPercentages()
            );
            
            log.debug("Created LeagueSummaryDTO for league: {}", league.getId());
            return dto;
        } catch (Exception e) {
            log.error("Error creating LeagueSummaryDTO for league {}: {}", league.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create league summary: " + e.getMessage(), e);
        }
    }

    /**
     * Hides a league (soft delete). Only the owner can hide their league.
     * Hidden leagues are not shown in lists but all data is preserved.
     * 
     * @param leagueId the ID of the league to hide
     * @param user the user attempting to hide the league
     * @throws LeagueNotFoundException if the league doesn't exist
     * @throws UnauthorizedException if the user is not the owner of the league
     */
    @Transactional
    public void hideLeague(Long leagueId, User user) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new LeagueNotFoundException(leagueId));

        // Only the owner can hide the league
        if (league.getOwner() == null || !league.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the league owner can hide the league");
        }

        log.info("Hiding league {} by user {}", leagueId, user.getId());

        // Set hidden flag instead of deleting
        league.setHidden(true);
        leagueRepository.save(league);

        log.info("Successfully hid league {}", leagueId);
    }

    /**
     * Unhides a league. Only the owner can unhide their league.
     * 
     * @param leagueId the ID of the league to unhide
     * @param user the user attempting to unhide the league
     * @throws LeagueNotFoundException if the league doesn't exist
     * @throws UnauthorizedException if the user is not the owner of the league
     */
    @Transactional
    public void unhideLeague(Long leagueId, User user) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new LeagueNotFoundException(leagueId));

        // Only the owner can unhide the league
        if (league.getOwner() == null || !league.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the league owner can unhide the league");
        }

        log.info("Unhiding league {} by user {}", leagueId, user.getId());

        // Remove hidden flag
        league.setHidden(false);
        leagueRepository.save(league);

        log.info("Successfully unhid league {}", leagueId);
    }
}


