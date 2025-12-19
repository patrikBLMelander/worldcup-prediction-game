package com.worldcup.repository;

import com.worldcup.entity.League;
import com.worldcup.entity.LeagueMembership;
import com.worldcup.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueMembershipRepository extends JpaRepository<LeagueMembership, Long> {

    Optional<LeagueMembership> findByLeagueAndUser(League league, User user);

    List<LeagueMembership> findByUser(User user);

    @EntityGraph(attributePaths = {"user"})
    List<LeagueMembership> findByLeague(League league);
}


