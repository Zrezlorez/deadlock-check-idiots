package com.litovskiy.service.log;

import com.litovskiy.entity.ActionLog;
import com.litovskiy.entity.PlayerGrowthStats;
import com.litovskiy.log.Action;
import com.litovskiy.log.LogMetadata;
import com.litovskiy.log.LogTag;
import com.litovskiy.log.metadata.GrowLogMetadata;
import com.litovskiy.repository.ActionLogRepository;
import com.litovskiy.util.GrowthCalculation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameLogService {
    private final ActionLogRepository actionLogRepository;
    private final GameLogMetadataMapper mapper;

    public List<ActionLog> findByActorId(Long actorId, int page, int size) {
        return actionLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, PageRequest.of(page, size));
    }

    public List<ActionLog> findByTargetId(Long targetId, int page, int size) {
        return actionLogRepository.findByTargetIdOrderByCreatedAtDesc(targetId, PageRequest.of(page, size));
    }

    public void saveGrowLog(Long playerId, GrowthCalculation growthCalculation, PlayerGrowthStats stat, boolean isScheduledMessage) {
        ActionLog actionLog = new ActionLog();
        actionLog.setActorId(playerId);
        actionLog.setOldActorSize(growthCalculation.oldValue());
        actionLog.setNewActorSize(growthCalculation.newValue());
        actionLog.setAction(Action.GROW);
        actionLog.getTags().add(Action.GROW.getLogTag());
        GrowLogMetadata logMetadata = new GrowLogMetadata(
            growthCalculation.diff(),
            growthCalculation.outcome(),
            growthCalculation.baseGrowth(),
            growthCalculation.activityBonus(),
            growthCalculation.slowdown(),
            growthCalculation.failChance(),
            growthCalculation.critChance(),
            growthCalculation.growthModifier(),
            growthCalculation.modifierBeforeOutcome(),
            growthCalculation.modifierAfterOutcome(),
            growthCalculation.finalModifier(),
            isScheduledMessage
        );
        actionLog.setMetadata(mapper.toJson(logMetadata));

        switch (growthCalculation.outcome()) {
            case CRIT -> actionLog.getTags().add(LogTag.CRITICAL_GROWTH);
            case FAIL -> actionLog.getTags().add(LogTag.FAILED_GROWTH);
            case NORMAL -> {
                actionLog.getTags().add(LogTag.NORMAL_GROWTH);
                // TODO: вынести в константы
                if (growthCalculation.finalModifier() < 1.08) {
                    actionLog.getTags().add(LogTag.LOW_GROWTH);
                }
                if (growthCalculation.finalModifier() > 1.15) {
                    actionLog.getTags().add(LogTag.HIGH_GROWTH);
                }
            }
        }

        if (stat.getCurrentFailStreak() > 1) {
            actionLog.getTags().add(LogTag.FAIL_STREAK);
        }

        if (stat.getCurrentLuckyStreak() > 1) {
            actionLog.getTags().add(LogTag.LUCKY_STREAK);
        }

        if (stat.getCurrentNormalStreak() > 1) {
            actionLog.getTags().add(LogTag.NORMAL_STREAK);
        }

        if (isScheduledMessage) {
            actionLog.getTags().add(LogTag.OFFLINE_GROWTH);
        }

        actionLogRepository.save(actionLog);
    }

    // TODO: добавить кеш и проверку на место в топе
    public void logAbility(Long playerId, Double oldActorSize, Double newActorSize,
                           Long targetId, Double oldTargetSize, Double newTargetSize,
                           Action action, LogMetadata logMetadata) {
        ActionLog actionLog = new ActionLog();
        actionLog.setActorId(playerId);
        actionLog.setOldActorSize(oldActorSize);
        if (newActorSize != null) {
            actionLog.setNewActorSize(newActorSize);
        }
        actionLog.setOldTargetSize(oldTargetSize);
        actionLog.setNewTargetSize(newTargetSize);
        actionLog.setTargetId(targetId);
        actionLog.setAction(action);
        actionLog.setMetadata(mapper.toJson(logMetadata));
        applyAbilityTags(actionLog, action);
        actionLogRepository.save(actionLog);
    }

    private void applyAbilityTags(ActionLog log, Action action) {
        switch (action) {
            case FUCK, SLOW -> log.getTags().add(LogTag.AGGRESSIVE_ACTION);

            case PRAY, TURTLE -> log.getTags().add(LogTag.SELF_BUFF_ACTION);

            case JACKPOT -> {
                log.getTags().add(LogTag.SELF_BUFF_ACTION);
                log.getTags().add(LogTag.RISK_ACTION);
            }

            case TRANSFER -> log.getTags().add(LogTag.SUPPORTIVE_ACTION);

            default -> {
            }
        }

        log.getTags().add(action.getLogTag());
    }
}
