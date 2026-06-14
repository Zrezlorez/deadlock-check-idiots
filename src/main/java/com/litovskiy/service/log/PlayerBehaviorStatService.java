package com.litovskiy.service.log;

import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.repository.PlayerBehaviorStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerBehaviorStatService {
    private final PlayerBehaviorStatRepository repository;

    @Transactional
    public PlayerBehaviorStats applyAbility(Long playerId, Action action, Long targetId) {
        PlayerBehaviorStats stat = repository.findByPlayerId(playerId).orElse(new PlayerBehaviorStats(playerId));

        stat.applyAbility(action, targetId);
        repository.save(stat);
        return stat;
    }



    public void save(PlayerBehaviorStats stats) {
        repository.save(stats);
    }

}
