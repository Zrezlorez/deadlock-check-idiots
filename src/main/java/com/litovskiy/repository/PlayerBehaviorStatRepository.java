package com.litovskiy.repository;

import com.litovskiy.entity.PlayerBehaviorStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerBehaviorStatRepository extends JpaRepository<PlayerBehaviorStats, Long> {
    Optional<PlayerBehaviorStats> findByPlayerId(Long playerId);
}
