package com.litovskiy.service.activity;

import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.VoiceSession;
import com.litovskiy.repository.VoiceSessionRepository;
import com.litovskiy.service.GameConfigService;
import com.litovskiy.util.GameSetting;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.data.ActivityStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class ActivityBonusService {

    private final VoiceSessionRepository voiceSessionRepository;
    private final PlayerAccountService playerAccountService;
    private final GameConfigService gameConfigService;
    private final ActivityStatService activityStatService;
    private final Clock clock;

    public double getGrowthBonusMultiplier(Platform platform, long profileId, Long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        int lookbackDays = gameConfigService.getInt(GameSetting.ACTIVITY_LOOKBACK_DAYS);
        double maxGrowthBonus = gameConfigService.getDouble(GameSetting.ACTIVITY_MAX_GROWTH_BONUS);
        LocalDate fromDate = LocalDate.now(clock).minusDays(lookbackDays - 1L);

        double telegramShare = getPlatformShare(player.getId(), Platform.TELEGRAM, fromDate,
            platform == Platform.TELEGRAM ? scopeId : null);
        double discordShare = getPlatformShare(player.getId(), Platform.DISCORD, fromDate,
            platform == Platform.DISCORD ? scopeId : null);

        return 1.0 + maxGrowthBonus * telegramShare + maxGrowthBonus * discordShare;
    }

    private double getPlatformShare(long playerChatId, Platform platform, LocalDate fromDate, Long preferredScopeId) {
        Set<Long> scopeIds = new LinkedHashSet<>();
        if (preferredScopeId != null) {
            scopeIds.add(preferredScopeId);
        } else {
            scopeIds.addAll(activityStatService.findScopeIdsByPlayer(playerChatId, platform, fromDate));
            if (platform == Platform.DISCORD) {
                scopeIds.addAll(voiceSessionRepository.findActiveScopeIdsByPlayer(playerChatId, platform));
            }
        }

        if (scopeIds.isEmpty()) {
            return 0.0;
        }

        double bestShare = 0.0;
        for (Long scopeId : scopeIds) {
            List<Double> shares = new ArrayList<>();
            addShare(shares, playerChatId, activityStatService.findMessageTotals(platform, scopeId, fromDate));

            if (platform == Platform.DISCORD) {
                Map<Long, Long> voiceTotals = new HashMap<>(activityStatService.findVoiceTotals(platform, scopeId, fromDate));
                for (VoiceSession session : voiceSessionRepository.findByPlatformAndScopeId(platform, scopeId)) {
                    voiceTotals.merge(
                        session.getId(),
                        getActiveSecondsInWindow(session, fromDate, LocalDateTime.now(clock)),
                        Long::sum
                    );
                }
                addShare(shares, playerChatId, voiceTotals);
            }

            if (!shares.isEmpty()) {
                double scopeShare = shares.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                bestShare = Math.max(bestShare, scopeShare);
            }
        }

        return bestShare;
    }


    private void addShare(List<Double> shares, long playerChatId, Map<Long, Long> totals) {
        long maxValue = totals.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        if (maxValue <= 0) {
            return;
        }

        long playerValue = totals.getOrDefault(playerChatId, 0L);
        shares.add((double) playerValue / maxValue);
    }



    private long getActiveSecondsInWindow(VoiceSession session, LocalDate fromDate, LocalDateTime now) {
        LocalDateTime windowStart = fromDate.atStartOfDay();
        LocalDateTime startedAt = session.getStartedAt().isAfter(windowStart) ? session.getStartedAt() : windowStart;
        if (!now.isAfter(startedAt)) {
            return 0;
        }

        return Duration.between(startedAt, now).getSeconds();
    }
}
