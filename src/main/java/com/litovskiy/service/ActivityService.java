package com.litovskiy.service;

import com.litovskiy.dao.ActivityStatDao;
import com.litovskiy.dao.VoiceSessionDao;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.VoiceSession;

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

public class ActivityService {

    private final PlayerAccountService playerAccountService;
    private final ActivityStatDao activityStatDao;
    private final VoiceSessionDao voiceSessionDao;
    private final GameConfigService gameConfigService;
    private final Clock clock;

    public ActivityService(PlayerAccountService playerAccountService,
                           ActivityStatDao activityStatDao,
                           VoiceSessionDao voiceSessionDao,
                           GameConfigService gameConfigService) {
        this(playerAccountService, activityStatDao, voiceSessionDao, gameConfigService, Clock.systemDefaultZone());
    }

    public ActivityService(PlayerAccountService playerAccountService,
                           ActivityStatDao activityStatDao,
                           VoiceSessionDao voiceSessionDao,
                           GameConfigService gameConfigService,
                           Clock clock) {
        this.playerAccountService = playerAccountService;
        this.activityStatDao = activityStatDao;
        this.voiceSessionDao = voiceSessionDao;
        this.gameConfigService = gameConfigService;
        this.clock = clock;
    }

    public void recordMessage(Platform platform, long profileId, long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        activityStatDao.incrementMessages(player.getChatId(), platform, scopeId, LocalDate.now(clock), 1);
    }

    public void startVoiceSession(Platform platform, long profileId, long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        LocalDateTime now = LocalDateTime.now(clock);

        VoiceSession activeSession = voiceSessionDao.findActiveSession(player.getChatId(), platform);
        if (activeSession == null) {
            voiceSessionDao.save(new VoiceSession(player.getChatId(), platform, scopeId, now));
            return;
        }

        if (activeSession.getScopeId().equals(scopeId)) {
            return;
        }

        closeSession(activeSession, now);
        voiceSessionDao.save(new VoiceSession(player.getChatId(), platform, scopeId, now));
    }

    public void endVoiceSession(Platform platform, long profileId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        VoiceSession activeSession = voiceSessionDao.findActiveSession(player.getChatId(), platform);
        if (activeSession == null) {
            return;
        }

        closeSession(activeSession, LocalDateTime.now(clock));
    }

    public double getGrowthBonusMultiplier(Platform platform, long profileId, Long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        int lookbackDays = gameConfigService.getInt(GameSetting.ACTIVITY_LOOKBACK_DAYS);
        double maxGrowthBonus = gameConfigService.getDouble(GameSetting.ACTIVITY_MAX_GROWTH_BONUS);
        LocalDate fromDate = LocalDate.now(clock).minusDays(lookbackDays - 1L);

        double telegramShare = getPlatformShare(player.getChatId(), Platform.TELEGRAM, fromDate,
            platform == Platform.TELEGRAM ? scopeId : null);
        double discordShare = getPlatformShare(player.getChatId(), Platform.DISCORD, fromDate,
            platform == Platform.DISCORD ? scopeId : null);

        return 1.0 + maxGrowthBonus * telegramShare + maxGrowthBonus * discordShare;
    }

    private double getPlatformShare(long playerChatId, Platform platform, LocalDate fromDate, Long preferredScopeId) {
        Set<Long> scopeIds = new LinkedHashSet<>();
        if (preferredScopeId != null) {
            scopeIds.add(preferredScopeId);
        } else {
            scopeIds.addAll(activityStatDao.findScopeIdsByPlayer(playerChatId, platform, fromDate));
            if (platform == Platform.DISCORD) {
                scopeIds.addAll(voiceSessionDao.findActiveScopeIdsByPlayer(playerChatId, platform));
            }
        }

        if (scopeIds.isEmpty()) {
            return 0.0;
        }

        double bestShare = 0.0;
        for (Long scopeId : scopeIds) {
            List<Double> shares = new ArrayList<>();
            addShare(shares, playerChatId, activityStatDao.findMessageTotals(platform, scopeId, fromDate));

            if (platform == Platform.DISCORD) {
                Map<Long, Long> voiceTotals = new HashMap<>(activityStatDao.findVoiceTotals(platform, scopeId, fromDate));
                for (VoiceSession session : voiceSessionDao.findActiveSessions(platform, scopeId)) {
                    voiceTotals.merge(
                        session.getPlayerChatId(),
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

    private void closeSession(VoiceSession session, LocalDateTime endedAt) {
        if (!endedAt.isAfter(session.getStartedAt())) {
            voiceSessionDao.delete(session);
            return;
        }

        persistVoiceDuration(session.getPlayerChatId(), session.getPlatform(), session.getScopeId(), session.getStartedAt(), endedAt);
        voiceSessionDao.delete(session);
    }

    private void persistVoiceDuration(long playerChatId,
                                      Platform platform,
                                      long scopeId,
                                      LocalDateTime startedAt,
                                      LocalDateTime endedAt) {
        LocalDateTime cursor = startedAt;

        while (cursor.toLocalDate().isBefore(endedAt.toLocalDate())) {
            LocalDateTime dayEnd = cursor.toLocalDate().plusDays(1).atStartOfDay();
            activityStatDao.incrementVoiceSeconds(
                playerChatId,
                platform,
                scopeId,
                cursor.toLocalDate(),
                Duration.between(cursor, dayEnd).getSeconds()
            );
            cursor = dayEnd;
        }

        long remainingSeconds = Duration.between(cursor, endedAt).getSeconds();
        if (remainingSeconds > 0) {
            activityStatDao.incrementVoiceSeconds(
                playerChatId,
                platform,
                scopeId,
                cursor.toLocalDate(),
                remainingSeconds
            );
        }
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
