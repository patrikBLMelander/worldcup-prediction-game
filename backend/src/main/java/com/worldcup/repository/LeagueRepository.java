package com.worldcup.repository;

import com.worldcup.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {

    Optional<League> findByJoinCode(String joinCode);
}


