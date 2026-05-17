package com.litovskiy.service;

import com.litovskiy.service.activity.ActivityService;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static com.litovskiy.entity.GrowthStyle.convertValue;
import static com.litovskiy.util.StringUtil.round;

@Service
@RequiredArgsConstructor
public class GrowService {

    private final PlayerService playerDao;
    private final PlayerAccountService playerAccountService;
    private final ActivityService activityService;
    private final ConversationStyleService conversationStyleService;
    private final GameConfigService gameConfigService;
    private final Random random;

    public String grow(Platform platform, long profileId, Long scopeId) {
        LocalDateTime now = LocalDateTime.now();
        Player player = playerAccountService.resolveOrCreate(platform, profileId);

        LocalDateTime lastTime = player.getLastGrowTime();
        if (lastTime != null) {
            int timeRange = gameConfigService.getInt(GameSetting.COOLDOWN_RANGE);
            ChronoUnit timeUnit = gameConfigService.getChronoUnit(GameSetting.COOLDOWN_UNIT);
            LocalDateTime nextAllowed = lastTime.plus(timeRange, timeUnit);

            Duration duration = Duration.between(now, nextAllowed);
            StringBuilder stringBuilder = new StringBuilder();
            if (duration.toHours() > 0) {
                stringBuilder.append(duration.toHours()).append(" ч ");
            }
            if (duration.toMinutesPart() > 0) {
                stringBuilder.append(duration.toMinutesPart()).append(" мин ");
            }
            stringBuilder.append(duration.toSecondsPart()).append(" сек ");
            if (now.isBefore(nextAllowed)) {
                return "Вы уже растили показатель, следующая попытка будет через:\n" + stringBuilder;
            }
        }

        double oldValue = player.getSize();
        double activityBonus = activityService == null ? 1.0 : activityService.getGrowthBonusMultiplier(platform, profileId, scopeId);
        GrowthStyle style = conversationStyleService == null
            ? GrowthStyle.DICK
            : conversationStyleService.getStyle(platform, scopeId);

        GrowthContext growthContext = consumePendingEffects(player);
        GrowthResult growthResult = resolveGrowthResult(oldValue, activityBonus, growthContext);
        player.setSize(growthResult.newValue());
        player.setLastGrowTime(now);
        playerDao.save(player);

        return buildResultMessage(style, oldValue, growthResult);
    }

    private GrowthResult resolveGrowthResult(double oldValue, double activityBonus, GrowthContext growthContext) {
        double outcomeRoll = random.nextDouble();
        double failChance = clampChance(gameConfigService.getDouble(GameSetting.FAIL_CHANCE) + growthContext.failChanceBonus());
        double critChance = clampChance(gameConfigService.getDouble(GameSetting.CRIT_CHANCE) + growthContext.critChanceBonus());
        critChance = Math.min(critChance, 1.0 - failChance);

        if (outcomeRoll < failChance) {
            return buildFailResult(oldValue);
        }

        double growthMultiplier = getGrowth(oldValue, activityBonus);
        boolean crit = outcomeRoll < failChance + critChance;
        if (crit) {
            double critMultiplier = gameConfigService.getDouble(GameSetting.CRIT_MULTIPLIER);
            growthMultiplier = 1 + (growthMultiplier - 1) * critMultiplier;
        }
        growthMultiplier = 1 + (growthMultiplier - 1) * (1 - growthContext.growthPenalty()) * (1 + growthContext.growthBonus);

        double newValue = round(oldValue * growthMultiplier);
        return new GrowthResult(newValue, crit ? Outcome.CRIT : Outcome.NORMAL);
    }

    private GrowthResult buildFailResult(double oldValue) {
        double failPercent = gameConfigService.getDouble(GameSetting.FAIL_PERCENT);
        double decreasedValue = round(oldValue * (1 - failPercent));
        double newValue = Math.max(0.0, decreasedValue);
        return new GrowthResult(newValue, Outcome.FAIL);
    }

    private String buildResultMessage(GrowthStyle style, double oldValue, GrowthResult growthResult) {
        double diff = round(Math.abs(growthResult.newValue() - oldValue));
        return switch (growthResult.outcome()) {
            case FAIL -> buildFailMessage(style, oldValue, growthResult.newValue(), diff);
            case CRIT -> String.format(
                "Джекпот! Ваш %s вырос на %s. Текущий размер: %s",
                style.getDisplayName(),
                convertValue(diff, style),
                convertValue(growthResult.newValue(), style)
            );
            case NORMAL -> String.format(
                "Ваш %s вырос на %s. Текущий размер: %s",
                style.getDisplayName(),
                convertValue(diff, style),
                convertValue(growthResult.newValue(), style)
            );
        };
    }

    private String buildFailMessage(GrowthStyle style, double oldValue, double newValue, double diff) {
        if (Double.compare(oldValue, newValue) == 0) {
            return String.format(
                "Неудача, но не страшно. Ваш %s не смог уменьшиться ниже нуля. Текущий размер: %s",
                style.getDisplayName(),
                convertValue(newValue, style)
            );
        }

        return String.format(
            "Неудача. Ваш %s уменьшился на %s. Текущий размер: %s",
            style.getDisplayName(),
            convertValue(diff, style),
            convertValue(newValue, style)
        );
    }

    private double getGrowth(double currentSize, double activityBonus) {
        double growthMean = gameConfigService.getDouble(GameSetting.GROWTH_MEAN);
        double minGrowth = gameConfigService.getDouble(GameSetting.GROWTH_MIN);
        double maxGrowth = gameConfigService.getDouble(GameSetting.GROWTH_MAX);
        double slowScale = gameConfigService.getDouble(GameSetting.SLOW_SCALE);
        double baseGrowth = growthMean + random.nextGaussian() * 0.01;
        baseGrowth = Math.max(minGrowth, Math.min(maxGrowth, baseGrowth));
        double slowdown = 1 / (1 + currentSize / slowScale);
        return 1 + (baseGrowth - 1) * slowdown * activityBonus;
    }

    private GrowthContext consumePendingEffects(Player player) {
        GrowthContext context = new GrowthContext(
            player.getPendingFailChancePenalty(),
            player.getPendingCritChanceBonus(),
            player.getPendingGrowthPenalty(),
            player.getPendingGrowthBonus()
        );
        player.setPendingFailChancePenalty(0.0);
        player.setPendingCritChanceBonus(0.0);
        player.setPendingGrowthPenalty(0.0);
        player.setPendingGrowthBonus(0.0);
        return context;
    }

    private double clampChance(double value) {
        return Math.max(0.0, Math.min(0.95, value));
    }

    private enum Outcome {
        NORMAL,
        CRIT,
        FAIL
    }

    private record GrowthResult(double newValue, Outcome outcome) {
    }

    private record GrowthContext(double failChanceBonus, double critChanceBonus, double growthPenalty, double growthBonus) {
    }
}
