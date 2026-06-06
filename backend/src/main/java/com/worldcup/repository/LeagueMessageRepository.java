package com.worldcup.repository;

import com.worldcup.entity.League;
import com.worldcup.entity.LeagueMessage;
import com.worldcup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeagueMessageRepository extends JpaRepository<LeagueMessage, Long> {

    /** Most recent messages for a league (newest first; reverse for display). */
    List<LeagueMessage> findTop200ByLeagueOrderByCreatedAtDesc(League league);

    /** Unread messages for a user in a league: posted after a baseline, by someone else. */
    long countByLeagueAndCreatedAtAfterAndUserNot(League league, LocalDateTime after, User user);
}
