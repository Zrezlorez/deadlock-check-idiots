package com.litovskiy.service.activity;

import com.litovskiy.entity.Platform;
import com.litovskiy.entity.VoiceSession;
import com.litovskiy.repository.VoiceSessionRepository;
import com.litovskiy.service.data.ActivityStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoiceSessionService {

    private final VoiceSessionRepository voiceSessionRepository;
    private final ActivityStatService activityStatService;
    private final Clock clock;

    @Transactional
    public void startSession(long playerId, Platform platform, long scopeId) {
        LocalDateTime now = LocalDateTime.now(clock);

        VoiceSession activeSession = voiceSessionRepository
            .findByPlatformAndPlayerId(platform, playerId)
            .orElse(null);

        if (activeSession == null) {
            voiceSessionRepository.save(new VoiceSession(playerId, platform, scopeId, now));
            return;
        }

        if (activeSession.getScopeId().equals(scopeId)) {
            return;
        }

        closeSession(activeSession, now);
        voiceSessionRepository.save(new VoiceSession(playerId, platform, scopeId, now));
    }

    @Transactional
    public void endSession(long playerId, Platform platform) {
        VoiceSession activeSession = voiceSessionRepository
            .findByPlatformAndPlayerId(platform, playerId)
            .orElse(null);

        if (activeSession == null) {
            return;
        }

        closeSession(activeSession, LocalDateTime.now(clock));
    }

    private void closeSession(VoiceSession session, LocalDateTime endedAt) {
        if (!endedAt.isAfter(session.getStartedAt())) {
            voiceSessionRepository.delete(session);
            return;
        }

        persistVoiceDuration(
            session.getPlayerId(),
            session.getPlatform(),
            session.getScopeId(),
            session.getStartedAt(),
            endedAt
        );

        voiceSessionRepository.delete(session);
    }

    private void persistVoiceDuration(
        long playerId,
        Platform platform,
        long scopeId,
        LocalDateTime startedAt,
        LocalDateTime endedAt
    ) {
        LocalDateTime cursor = startedAt;

        while (cursor.toLocalDate().isBefore(endedAt.toLocalDate())) {
            LocalDateTime dayEnd = cursor.toLocalDate().plusDays(1).atStartOfDay();

            activityStatService.incrementVoiceSeconds(
                playerId,
                platform,
                scopeId,
                cursor.toLocalDate(),
                Duration.between(cursor, dayEnd).getSeconds()
            );

            cursor = dayEnd;
        }

        long remainingSeconds = Duration.between(cursor, endedAt).getSeconds();

        if (remainingSeconds > 0) {
            activityStatService.incrementVoiceSeconds(
                playerId,
                platform,
                scopeId,
                cursor.toLocalDate(),
                remainingSeconds
            );
        }
    }
}
