package com.litovskiy.service.activity;

import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.data.ActivityStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final PlayerAccountService playerAccountService;
    private final ActivityStatService activityStatService;
    private final VoiceSessionService voiceSessionService;
    private final ActivityBonusService activityBonusService;
    private final Clock clock;

    @Transactional
    public void recordMessage(Platform platform, long profileId, long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        activityStatService.incrementMessages(
            player.getId(),
            platform,
            scopeId,
            LocalDate.now(clock),
            1
        );
    }

    @Transactional
    public void startVoiceSession(Platform platform, long profileId, long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        voiceSessionService.startSession(player.getId(), platform, scopeId);
    }

    @Transactional
    public void endVoiceSession(Platform platform, long profileId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        voiceSessionService.endSession(player.getId(), platform);
    }

    @Transactional()
    public double getGrowthBonusMultiplier(Platform platform, long profileId, Long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        return activityBonusService.getGrowthBonusMultiplier(platform, player.getId(), scopeId);
    }
}
