package com.worldcup.repository;

import com.worldcup.entity.Match;
import com.worldcup.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStatus(MatchStatus status);
    List<Match> findByMatchDateBetween(LocalDateTime start, LocalDateTime end);
    List<Match> findByGroup(String group);
    List<Match> findByStatusOrderByMatchDateAsc(MatchStatus status);
    List<Match> findByStatusAndMatchDateAfter(MatchStatus status, LocalDateTime date);
}


