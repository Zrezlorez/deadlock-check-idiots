package com.litovskiy.service.data;

import com.litovskiy.entity.ActivityStat;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.PlayerTotalProjection;
import com.litovskiy.repository.ActivityStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityStatService {

    private final ActivityStatRepository repository;

    @Transactional
    public void deleteByPlayerId(long playerId) {
        repository.deleteByPlayerId(playerId);
    }

    @Transactional
    public void incrementMessages(long playerId, Platform platform, long scopeId, LocalDate date, long amount) {
        repository.incrementMessages(playerId, platform.name(), scopeId, date, amount);
    }

    @Transactional
    public void incrementVoiceSeconds(
        long playerChatId,
        Platform platform,
        long scopeId,
        LocalDate activityDate,
        long seconds
    ) {
        ActivityStat stat = repository
            .findByPlayerIdAndPlatformAndScopeIdAndActivityDate(
                playerChatId,
                platform,
                scopeId,
                activityDate
            )
            .orElseGet(() -> new ActivityStat(
                playerChatId,
                platform,
                scopeId,
                activityDate,
                0,
                0
            ));

        stat.setVoiceSeconds(stat.getVoiceSeconds() + seconds);

        repository.save(stat);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> findMessageTotals(
        Platform platform,
        long scopeId,
        LocalDate fromDate
    ) {
        return toMap(repository.findMessageTotalRows(platform, scopeId, fromDate));
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> findVoiceTotals(
        Platform platform,
        long scopeId,
        LocalDate fromDate
    ) {
        return toMap(repository.findVoiceTotalRows(platform, scopeId, fromDate));
    }

    @Transactional(readOnly = true)
    public List<Long> findScopeIdsByPlayer(
        long playerChatId,
        Platform platform,
        LocalDate fromDate
    ) {
        return repository.findScopeIdsByPlayer(playerChatId, platform, fromDate);
    }

    private Map<Long, Long> toMap(List<PlayerTotalProjection> rows) {
        return rows.stream()
            .collect(Collectors.toMap(
                PlayerTotalProjection::getPlayerId,
                PlayerTotalProjection::getTotal
            ));
    }
}
