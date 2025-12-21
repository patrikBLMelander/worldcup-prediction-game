package com.worldcup.service;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import com.worldcup.exception.MatchNotFoundException;
import com.worldcup.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;

    public Match createMatch(String homeTeam, String awayTeam, LocalDateTime matchDate, 
                            String venue, String group) {
        Match match = new Match();
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setMatchDate(matchDate);
        match.setVenue(venue);
        match.setGroup(group);
        match.setStatus(MatchStatus.SCHEDULED);

        return matchRepository.save(match);
    }

    public Optional<Match> findById(Long id) {
        return matchRepository.findById(id);
    }

    public List<Match> findAll() {
        return matchRepository.findAll();
    }

    public List<Match> findByStatus(MatchStatus status) {
        return matchRepository.findByStatus(status);
    }

    public List<Match> findByGroup(String group) {
        return matchRepository.findByGroup(group);
    }

    public Match updateMatchResult(Long matchId, Integer homeScore, Integer awayScore) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setStatus(MatchStatus.FINISHED);

        return matchRepository.save(match);
    }

    public Match updateMatchStatus(Long matchId, MatchStatus status) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));

        match.setStatus(status);
        return matchRepository.save(match);
    }

    public void deleteMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new MatchNotFoundException(matchId));
        matchRepository.delete(match);
    }
}

