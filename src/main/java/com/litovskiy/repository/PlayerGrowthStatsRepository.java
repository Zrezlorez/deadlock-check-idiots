package com.litovskiy.repository;

import com.litovskiy.entity.PlayerGrowthStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerGrowthStatsRepository extends JpaRepository<PlayerGrowthStats, Integer> {

    Optional<PlayerGrowthStats> findByPlayerId(Long playerId);
}
