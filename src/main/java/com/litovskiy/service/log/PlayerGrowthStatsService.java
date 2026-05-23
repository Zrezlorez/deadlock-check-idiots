package com.litovskiy.service.log;

import com.litovskiy.entity.PlayerGrowthStats;
import com.litovskiy.repository.PlayerGrowthStatsRepository;
import com.litovskiy.util.GrowOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerGrowthStatsService {
    private final PlayerGrowthStatsRepository repository;

    public PlayerGrowthStats logGrowthStats(Long playerId, GrowOutcome outcome) {
        PlayerGrowthStats stat = findByPlayerId(playerId);
        if (outcome == GrowOutcome.CRIT) {
            stat.increaseCurrentLuckyStreak();
            stat.increaseTotalCrits();
            stat.setCurrentFailStreak(0);
            stat.setCurrentNormalStreak(0);
            if (stat.getCurrentLuckyStreak() > stat.getMaxLuckyStreak()) {
                stat.setMaxFailStreak(stat.getCurrentLuckyStreak());
            }
        }
        if (outcome == GrowOutcome.FAIL) {
            stat.increaseCurrentFailStreak();
            stat.increaseTotalFails();
            stat.setCurrentLuckyStreak(0);
            stat.setCurrentNormalStreak(0);
            if (stat.getCurrentFailStreak() > stat.getMaxFailStreak()) {
                stat.setMaxFailStreak(stat.getCurrentFailStreak());
            }
        }
        if (outcome == GrowOutcome.NORMAL) {
            stat.increaseCurrentNormalStreak();
            stat.increaseTotalNormalGrowths();
            stat.setCurrentLuckyStreak(0);
            stat.setCurrentFailStreak(0);
            if (stat.getCurrentNormalStreak() > stat.getMaxNormalStreak()) {
                stat.setMaxNormalStreak(stat.getCurrentNormalStreak());
            }
        }
        repository.save(stat);
        return stat;
    }

    public PlayerGrowthStats findByPlayerId(Long playerId) {
        return repository.findByPlayerId(playerId).orElse(new PlayerGrowthStats(playerId));
    }
}
