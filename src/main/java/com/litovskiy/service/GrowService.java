package com.litovskiy.service;

import com.litovskiy.config.properties.GrowthProperties;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.PlayerGrowthStats;
import com.litovskiy.service.ability.AbilityService;
import com.litovskiy.service.ability.PlayerStatusService;
import com.litovskiy.service.activity.ActivityService;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.service.log.GameLogService;
import com.litovskiy.service.log.PlayerGrowthStatsService;
import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.util.GrowOutcome;
import com.litovskiy.util.GrowthCalculation;
import com.litovskiy.util.GrowthContext;
import com.litovskiy.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import static com.litovskiy.entity.GrowthStyle.convertValue;
import static com.litovskiy.util.StringUtil.formatDuration;
import static com.litovskiy.util.StringUtil.round;

@Service
@RequiredArgsConstructor
public class GrowService {

    private final PlayerService playerDao;
    private final PlayerAccountService playerAccountService;
    private final ActivityService activityService;
    private final ConversationStyleService conversationStyleService;
    private final PlayerStatusService playerStatusService;
    private final PlayerGrowthStatsService playerGrowthStatsService;
    private final GameLogService gameLogService;
    private final Random random;
    private final GrowthProperties growthProperties;
    private final AbilityService abilityService;

    @Transactional
    public CommandResult grow(Platform platform, long playerId, Long scopeId, boolean isScheduledMessage) {
        Player player = playerAccountService.resolveOrCreate(platform, playerId);

        Optional<String> cooldown = getCooldownString(player);
        if (cooldown.isPresent()) {
            return CommandResult.single(cooldown.get());
        }

        GrowthStyle style = conversationStyleService.getStyle(platform, scopeId);
        GrowthCalculation growthCalculation = calculateGrowth(platform, scopeId, player, isScheduledMessage);


        PlayerGrowthStats stat = playerGrowthStatsService.logGrowthStats(playerId, growthCalculation.outcome());
        gameLogService.saveGrowLog(playerId, growthCalculation, stat, isScheduledMessage);


        player.setSize(growthCalculation.newValue());
        player.setLastGrowTime(LocalDateTime.now());
        playerDao.save(player);

        return CommandResult.single(buildResultMessage(style, growthCalculation));
    }

    public CommandResult buildProfileResponse(Platform platform, long playerId, Long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, playerId);
        GrowthStyle style = conversationStyleService.getStyle(platform, scopeId);

        String profileName = platform == Platform.TELEGRAM
                ? player.getTelegramDisplayName()
                : player.getDiscordTag();

        String text = """
                Профиль игрока %s
                
                %s: %s
                Активный статус: %s
                
                Время до следующего роста: %s
                Время до следующей способности: %s
                """.formatted(
                profileName,
                StringUtils.capitalize(style.getDisplayName()),
                convertValue(player.getSize(), style),
                playerStatusService.getActiveStatus(player).getName(),
                getCooldown(player).map(StringUtil::formatDuration).orElse("Доступно сейчас"),
                abilityService.getCooldown(player).map(StringUtil::formatDuration).orElse("Доступно сейчас"));

        return CommandResult.of(CommandMessage.reply(text));
    }

    public Optional<Duration> getCooldown(Player player) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = player.getLastGrowTime();
        if (lastTime != null) {
            LocalDateTime nextAllowed = lastTime.plus(growthProperties.getGrowthCooldown());
            if (now.isBefore(nextAllowed)) {
                return Optional.of(Duration.between(now, nextAllowed));
            }
        }
        return Optional.empty();
    }

    private Optional<String> getCooldownString(Player player) {
        return getCooldown(player).map(it -> "Вы уже растили показатель, следующая попытка будет через:\n" + formatDuration(it));
    }

    private GrowthCalculation calculateGrowth(Platform platform, Long scopeId, Player player, boolean isScheduledMessage) {
        double oldValue = player.getSize();

        double activityBonus = activityService.getGrowthBonusMultiplier(platform, player.getId(), scopeId);
        GrowthContext context = consumePendingEffects(player);
        context = playerStatusService.applyGrowthStatusEffects(player, context);

        double failChance = context.failChance();
        double critChance = context.critChance();

        if (isScheduledMessage) {
            failChance += growthProperties.getOfflineFailChance();
            critChance = 0;
        }

        failChance = clampChance(failChance);
        critChance = Math.min(critChance, 1.0 - failChance);

        GrowthBase base = calculateBaseGrowth(oldValue, activityBonus);

        double roll = random.nextDouble();

        if (roll < failChance) {
            return calculateFail(oldValue, base, context, failChance, critChance, activityBonus, isScheduledMessage);
        }

        boolean crit = roll < failChance + critChance;
        double modifierAfterOutcome = base.modifier();
        if (crit) {
            modifierAfterOutcome = 1.0 + (modifierAfterOutcome - 1.0) * context.critMultiplier();
        }

        double growthModifier = isScheduledMessage ? 0 : context.growthModifier();
        double finalModifier = 1.0 + (modifierAfterOutcome - 1.0) * (1.0 + growthModifier);

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
            context.growthModifier(),
            base.modifier(),
            modifierAfterOutcome,
            finalModifier
        );
    }

    private GrowthBase calculateBaseGrowth(double currentSize, double activityBonus) {
        double growthMean = growthProperties.getGrowthMean();
        double minGrowth = growthProperties.getGrowthMin();
        double maxGrowth = growthProperties.getGrowthMax();
        double growthLimit = growthProperties.getGrowthLimit();
        double growthGauss = growthProperties.getGrowthGauss();

        double baseGrowth = growthMean + random.nextGaussian() * growthGauss;
        baseGrowth = Math.clamp(baseGrowth, minGrowth, maxGrowth);

        double slowdown = 1.0 / (1.0 + currentSize / growthLimit);
        double modifier = 1.0 + (baseGrowth - 1.0) * slowdown * activityBonus;

        return new GrowthBase(baseGrowth, slowdown, modifier);
    }

    private GrowthCalculation calculateFail(
        double oldValue,
        GrowthBase base,
        GrowthContext context,
        double failChance,
        double critChance,
        double activityBonus,
        boolean isScheduledMessage
    ) {

        double failPercent = context.failPercent();
        if (isScheduledMessage) {
            failPercent += growthProperties.getOfflineFailPercent();
        }
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
        double critChance = growthProperties.getCritChance();
        double critMultiplier = growthProperties.getCritMultiplier();
        double failChance = growthProperties.getFailChance();
        double failPercent = growthProperties.getFailPercent();
        GrowthContext context = new GrowthContext(
            clampChance(critChance + player.getPendingCritChanceModifier()),
            critMultiplier,
            clampChance(failChance + player.getPendingFailChanceModifier()),
            failPercent,
            player.getPendingGrowthModifier()
        );
        player.setPendingFailChanceModifier(0.0);
        player.setPendingCritChanceModifier(0.0);
        player.setPendingGrowthModifier(0.0);
        return context;
    }

    private double clampChance(double value) {
        return Math.clamp(round(value), 0, 0.95);
    }

    private record GrowthBase(double baseGrowth, double slowdown, double modifier) {
    }
}
