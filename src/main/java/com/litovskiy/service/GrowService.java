package com.litovskiy.service;

import com.litovskiy.entity.PlayerGrowthStats;
import com.litovskiy.util.GameSetting;
import com.litovskiy.service.activity.ActivityService;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.log.GameLogService;
import com.litovskiy.service.log.PlayerGrowthStatsService;
import com.litovskiy.util.GrowOutcome;
import com.litovskiy.util.GrowthCalculation;
import com.litovskiy.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

import static com.litovskiy.entity.GrowthStyle.convertValue;
import static com.litovskiy.util.StringUtil.clamp;
import static com.litovskiy.util.StringUtil.formatDuration;
import static com.litovskiy.util.StringUtil.round;

@Service
@RequiredArgsConstructor
public class GrowService {

    private final PlayerService playerDao;
    private final PlayerAccountService playerAccountService;
    private final ActivityService activityService;
    private final ConversationStyleService conversationStyleService;
    private final GameConfigService gameConfigService;
    private final PlayerGrowthStatsService playerGrowthStatsService;
    private final GameLogService gameLogService;
    private final Random random;

    @Transactional
    public String grow(Platform platform, long playerId, Long scopeId) {
        LocalDateTime now = LocalDateTime.now();
        Player player = playerAccountService.resolveOrCreate(platform, playerId);

        Optional<String> cooldown = checkCooldown(player, now);
        if (cooldown.isPresent()) {
            return cooldown.get();
        }

        GrowthStyle style = conversationStyleService.getStyle(platform, scopeId);
        GrowthCalculation growthCalculation = calculateGrowth(platform, scopeId, player);


        PlayerGrowthStats stat = playerGrowthStatsService.logGrowthStats(playerId, growthCalculation.outcome());
        gameLogService.saveGrowLog(playerId, growthCalculation, stat);


        player.setSize(growthCalculation.newValue());
        player.setLastGrowTime(now);
        playerDao.save(player);

        return buildResultMessage(style, growthCalculation);
    }

    private Optional<String> checkCooldown(Player player, LocalDateTime now) {
        LocalDateTime lastTime = player.getLastGrowTime();
        if (lastTime != null) {
            int timeRange = gameConfigService.getInt(GameSetting.COOLDOWN_RANGE);
            ChronoUnit timeUnit = gameConfigService.getChronoUnit(GameSetting.COOLDOWN_UNIT);

            LocalDateTime nextAllowed = lastTime.plus(timeRange, timeUnit);
            Duration duration = Duration.between(now, nextAllowed);
            if (now.isBefore(nextAllowed)) {
                return Optional.of("Вы уже растили показатель, следующая попытка будет через:\n"
                    + formatDuration(duration));
            }
        }
        return Optional.empty();
    }

    private GrowthCalculation calculateGrowth(Platform platform, Long scopeId, Player player) {
        double oldValue = player.getSize();

        double activityBonus = activityService.getGrowthBonusMultiplier(platform, player.getId(), scopeId);
        GrowthContext context = consumePendingEffects(player);

        double failChance = clampChance(
            gameConfigService.getDouble(GameSetting.FAIL_CHANCE)
                + context.failChanceModifier()
        );

        double critChance = clampChance(
            gameConfigService.getDouble(GameSetting.CRIT_CHANCE)
                + context.critChanceModifier()
        );


        critChance = Math.min(critChance, 1.0 - failChance);

        GrowthBase base = calculateBaseGrowth(oldValue, activityBonus);

        double roll = random.nextDouble();

        if (roll < failChance) {
            return calculateFail(oldValue, base, context, failChance, critChance, activityBonus);
        }

        boolean crit = roll < failChance + critChance;
        double modifierAfterOutcome = base.modifier();
        if (crit) {
            double critMultiplier = gameConfigService.getDouble(GameSetting.CRIT_MULTIPLIER);
            modifierAfterOutcome = 1.0 + (modifierAfterOutcome - 1.0) * critMultiplier;
        }

        double finalModifier = 1.0 + (modifierAfterOutcome - 1.0) * (1.0 + context.growthModifier());

        double newValue = round(oldValue * finalModifier);
        double diff = round(Math.abs(newValue - oldValue));

        return new GrowthCalculation(
            oldValue,
            newValue,
            diff,
            crit ? GrowOutcome.CRIT : GrowOutcome.NORMAL,
            base.baseGrowth(),
            activityBonus,
            base.slowdown(),
            failChance,
            critChance,
            context.growthModifier,
            base.modifier(),
            modifierAfterOutcome,
            finalModifier
        );
    }

    private GrowthBase calculateBaseGrowth(double currentSize, double activityBonus) {
        double growthMean = gameConfigService.getDouble(GameSetting.GROWTH_MEAN);
        double minGrowth = gameConfigService.getDouble(GameSetting.GROWTH_MIN);
        double maxGrowth = gameConfigService.getDouble(GameSetting.GROWTH_MAX);
        double slowScale = gameConfigService.getDouble(GameSetting.SLOW_SCALE);

        double baseGrowth = growthMean + random.nextGaussian() * 0.01;
        baseGrowth = StringUtil.clamp(baseGrowth, minGrowth, maxGrowth);

        double slowdown = 1.0 / (1.0 + currentSize / slowScale);
        double modifier = 1.0 + (baseGrowth - 1.0) * slowdown * activityBonus;

        return new GrowthBase(baseGrowth, slowdown, modifier);
    }

    private GrowthCalculation calculateFail(
        double oldValue,
        GrowthBase base,
        GrowthContext context,
        double failChance,
        double critChance,
        double activityBonus
    ) {
        double failPercent = gameConfigService.getDouble(GameSetting.FAIL_PERCENT);
        double newValue = Math.max(0.0, round(oldValue * (1.0 - failPercent)));
        double diff = round(Math.abs(newValue - oldValue));

        double finalModifier = oldValue == 0.0 ? 1.0 : newValue / oldValue;

        return new GrowthCalculation(
            oldValue,
            newValue,
            diff,
            GrowOutcome.FAIL,
            base.baseGrowth(),
            activityBonus,
            base.slowdown(),
            failChance,
            critChance,
            context.growthModifier(),
            base.modifier(),
            finalModifier,
            finalModifier
        );
    }

    private String buildResultMessage(GrowthStyle style, GrowthCalculation growthCalculation) {
        return switch (growthCalculation.outcome()) {
            case FAIL -> buildFailMessage(style, growthCalculation.oldValue(), growthCalculation.newValue(), growthCalculation.diff());
            case CRIT -> String.format(
                "Джекпот! Ваш %s вырос на %s. Текущий размер: %s",
                style.getDisplayName(),
                convertValue(growthCalculation.diff(), style),
                convertValue(growthCalculation.newValue(), style)
            );
            case NORMAL -> String.format(
                "Ваш %s вырос на %s. Текущий размер: %s",
                style.getDisplayName(),
                convertValue(growthCalculation.diff(), style),
                convertValue(growthCalculation.newValue(), style)
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

    private GrowthContext consumePendingEffects(Player player) {
        GrowthContext context = new GrowthContext(
            player.getPendingFailChanceModifier(),
            player.getPendingCritChanceModifier(),
            player.getPendingGrowthModifier()
        );
        player.setPendingFailChanceModifier(0.0);
        player.setPendingCritChanceModifier(0.0);
        player.setPendingGrowthModifier(0.0);
        return context;
    }

    private double clampChance(double value) {
        return clamp(value, 0, 0.95);
    }

    private record GrowthBase(double baseGrowth, double slowdown, double modifier) {
    }

    private record GrowthContext(double failChanceModifier,
                                 double critChanceModifier,
                                 double growthModifier) {
    }
}
