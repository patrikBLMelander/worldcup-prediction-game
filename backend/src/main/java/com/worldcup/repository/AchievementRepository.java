package com.worldcup.repository;

import com.worldcup.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findByCode(String code);
    java.util.List<Achievement> findByActiveTrueOrderByCategoryAscRarityDesc();
}

