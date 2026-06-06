package com.worldcup.repository;

import com.worldcup.entity.League;
import com.worldcup.entity.LeagueChatRead;
import com.worldcup.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeagueChatReadRepository extends JpaRepository<LeagueChatRead, Long> {
    Optional<LeagueChatRead> findByLeagueAndUser(League league, User user);
}
