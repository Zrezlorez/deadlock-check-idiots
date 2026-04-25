package com.litovskiy.service;

import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import static com.litovskiy.util.StringUtil.convertValue;
import static com.litovskiy.util.StringUtil.round;

public class DickService {

    private final PlayerDao playerDao;
    private final PlayerAccountService playerAccountService;
    private final ActivityService activityService;
    private final ConversationStyleService conversationStyleService;
    private final GameConfigService gameConfigService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Random random;

    public DickService(PlayerDao playerDao,
                       PlayerAccountService playerAccountService,
                       ActivityService activityService,
                       ConversationStyleService conversationStyleService,
                       GameConfigService gameConfigService) {
        this(playerDao, playerAccountService, activityService, conversationStyleService, gameConfigService, new Random());
    }

    public DickService(PlayerDao playerDao,
                       PlayerAccountService playerAccountService,
                       ActivityService activityService,
                       ConversationStyleService conversationStyleService,
                       GameConfigService gameConfigService,
                       Random random) {
        this.playerDao = playerDao;
        this.playerAccountService = playerAccountService;
        this.activityService = activityService;
        this.conversationStyleService = conversationStyleService;
        this.gameConfigService = gameConfigService;
        this.random = random;
    }

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

        GrowthResult growthResult = resolveGrowthResult(oldValue, activityBonus);
        player.setSize(growthResult.newValue());
        player.setLastGrowTime(now);
        playerDao.save(player);

        return buildResultMessage(style, oldValue, growthResult);
    }

    private GrowthResult resolveGrowthResult(double oldValue, double activityBonus) {
        double outcomeRoll = random.nextDouble();
        double failChance = gameConfigService.getDouble(GameSetting.FAIL_CHANCE);
        double critChance = gameConfigService.getDouble(GameSetting.CRIT_CHANCE);

        if (outcomeRoll < failChance) {
            return buildFailResult(oldValue);
        }

        double growthMultiplier = getGrowth(oldValue, activityBonus);
        boolean crit = outcomeRoll < failChance + critChance;
        if (crit) {
            double critMultiplier = gameConfigService.getDouble(GameSetting.CRIT_MULTIPLIER);
            growthMultiplier = 1 + (growthMultiplier - 1) * critMultiplier;
        }

        double newValue = round(oldValue * growthMultiplier);
        return new GrowthResult(newValue, crit ? Outcome.CRIT : Outcome.NORMAL);
    }

    private GrowthResult buildFailResult(double oldValue) {
        double failPercent = gameConfigService.getDouble(GameSetting.FAIL_PERCENT);
        double minValue = gameConfigService.getDouble(GameSetting.START_SIZE);
        double decreasedValue = round(oldValue * (1 - failPercent));
        double newValue = Math.max(minValue, decreasedValue);
        return new GrowthResult(newValue, Outcome.FAIL);
    }

    private String buildResultMessage(GrowthStyle style, double oldValue, GrowthResult growthResult) {
        double diff = round(Math.abs(growthResult.newValue() - oldValue));
        return switch (growthResult.outcome()) {
            case FAIL -> buildFailMessage(style, oldValue, growthResult.newValue(), diff);
            case CRIT -> String.format(
                "Джекпот! Ваш %s вырос на %s. Текущий размер: %s",
                style.displayName(),
                convertValue(diff),
                convertValue(growthResult.newValue())
            );
            case NORMAL -> String.format(
                "Ваш %s вырос на %s. Текущий размер: %s",
                style.displayName(),
                convertValue(diff),
                convertValue(growthResult.newValue())
            );
        };
    }

    private String buildFailMessage(GrowthStyle style, double oldValue, double newValue, double diff) {
        if (Double.compare(oldValue, newValue) == 0) {
            return String.format(
                "Неудача, но не страшно. Ваш %s не смог уменьшиться ниже стартового размера. Текущий размер: %s",
                style.displayName(),
                convertValue(newValue)
            );
        }

        return String.format(
            "Неудача. Ваш %s уменьшился на %s. Текущий размер: %s",
            style.displayName(),
            convertValue(diff),
            convertValue(newValue)
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


    private enum Outcome {
        NORMAL,
        CRIT,
        FAIL
    }

    private record GrowthResult(double newValue, Outcome outcome) {
    }
}
