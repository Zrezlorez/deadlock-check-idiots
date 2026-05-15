package com.litovskiy.service;

import com.litovskiy.repository.ConversationParticipantRepository;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.litovskiy.util.StringUtil.round;

@Component
@RequiredArgsConstructor
public class AbilityService {

    private final PlayerService playerDao;
    private final PlayerAccountService playerAccountService;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final GameConfigService gameConfigService;
    private final ConversationStyleService conversationStyleService;
    private final Clock clock;


    public String fuck(Platform platform, long profileId, Long scopeId, long targetProfileId) {
        if (scopeId == null) {
            return "Эта способность доступна только в беседе или на сервере.";
        }

        Player actor = playerAccountService.resolveOrCreate(platform, profileId);
        String cooldownMessage = checkCooldown(actor);
        if (cooldownMessage != null) {
            return cooldownMessage;
        }

        Player target = playerAccountService.resolveOrCreate(platform, targetProfileId);
        String validationMessage = validateTarget(platform, scopeId, actor, target);
        if (validationMessage != null) {
            return validationMessage;
        }

        double costPercent = gameConfigService.getDouble(GameSetting.ENEMY_FAIL_COST_PERCENT);
        double failBonus = gameConfigService.getDouble(GameSetting.ENEMY_FAIL_CHANCE_PENALTY);
        double maxFailChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_FAIL_CHANCE_PENALTY);
        double cost = round(actor.getSize() * costPercent);

        actor.setSize(Math.max(0.0, round(actor.getSize() - cost)));
        actor.setLastAbilityTime(LocalDateTime.now(clock));
        target.setPendingFailChancePenalty(Math.min(
            maxFailChance,
            round(target.getPendingFailChancePenalty() + failBonus)
        ));

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        playerDao.save(target);
        playerDao.save(actor);
        return "Вы усилили шанс неудачи цели на " + toPercent(failBonus)
            + "% за " + GrowthStyle.convertValue(cost, growthStyle) + ". Следующая попытка роста у цели будет опаснее.";
    }

    public String slow(Platform platform, long profileId, Long scopeId, long targetProfileId) {
        if (scopeId == null) {
            return "Эта способность доступна только в беседе или на сервере.";
        }

        Player actor = playerAccountService.resolveOrCreate(platform, profileId);
        String cooldownMessage = checkCooldown(actor);
        if (cooldownMessage != null) {
            return cooldownMessage;
        }

        Player target = playerAccountService.resolveOrCreate(platform, targetProfileId);
        String validationMessage = validateTarget(platform, scopeId, actor, target);
        if (validationMessage != null) {
            return validationMessage;
        }

        double growthPenalty = gameConfigService.getDouble(GameSetting.ENEMY_GROWTH_PENALTY);
        double maxGrowthPenalty = gameConfigService.getDouble(GameSetting.MAX_PENDING_GROWTH_PENALTY);

        actor.setLastAbilityTime(LocalDateTime.now(clock));
        target.setPendingGrowthPenalty(Math.min(
            maxGrowthPenalty,
            round(target.getPendingGrowthPenalty() + growthPenalty)
        ));

        playerDao.save(target);
        playerDao.save(actor);
        return "Вы ослабили следующий рост цели на " + toPercent(growthPenalty) + "%.";
    }

    public String transfer(Platform platform, long profileId, Long scopeId, long targetProfileId, String value) {
        if (scopeId == null) {
            return "Эта способность доступна только в беседе или на сервере.";
        }

        double transferValue;
        try {
            transferValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return "После команды нужно указать число для перевода";
        }

        Player actor = playerAccountService.resolveOrCreate(platform, profileId);
        String cooldownMessage = checkCooldown(actor);
        if (cooldownMessage != null) {
            return cooldownMessage;
        }

        Player target = playerAccountService.resolveOrCreate(platform, targetProfileId);
        String validationMessage = validateTarget(platform, scopeId, actor, target);
        if (validationMessage != null) {
            return validationMessage;
        }

        if (actor.getSize() - 1 < transferValue) {
            return "Вы не можете перевести больше, чем у вас есть";
        }

        String name = "камута";
        if (platform == Platform.TELEGRAM) {
            name = target.getTelegramDisplayName();
        } else if (platform == Platform.DISCORD) {
            name = target.getDiscordTag();
        }

        double costPercent = gameConfigService.getDouble(GameSetting.ABILITY_TRANSFER_COMMISSION);
        double cost = round(transferValue * costPercent);

        actor.setSize(Math.max(1.0, round(actor.getSize() - transferValue)));
        actor.setLastAbilityTime(LocalDateTime.now(clock));
        target.setSize(Math.min(
            1.0,
            round(target.getSize() + transferValue - cost)
        ));

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        playerDao.save(target);
        playerDao.save(actor);
        return "Вы перевели " + GrowthStyle.convertValue(transferValue, growthStyle) + " "
            + name + " с комиссией в размере " + GrowthStyle.convertValue(cost, growthStyle);
    }

    public String jackpot(Platform platform, long profileId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        String cooldownMessage = checkCooldown(player);
        if (cooldownMessage != null) {
            return cooldownMessage;
        }

        double failBonus = gameConfigService.getDouble(GameSetting.SELF_FAIL_CHANCE_PENALTY);
        double critBonus = gameConfigService.getDouble(GameSetting.SELF_CRIT_CHANCE_BONUS);

        double maxCritChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_CRIT_CHANCE_BONUS);
        double maxFailChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_FAIL_CHANCE_PENALTY);


        player.setLastAbilityTime(LocalDateTime.now(clock));
        player.setPendingCritChanceBonus(Math.min(
            maxCritChance,
            round(player.getPendingCritChanceBonus() + critBonus)
        ));

        player.setPendingFailChancePenalty(Math.min(
            maxFailChance,
            round(player.getPendingFailChancePenalty() + failBonus)
        ));

        playerDao.save(player);
        return "Вы увеличили шанс джекпота на " + toPercent(critBonus) + "% и повысили шанс неудачи на " + toPercent(failBonus)
            + "% для следующего роста.";
    }

    public String turtle(Platform platform, long profileId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        String cooldownMessage = checkCooldown(player);
        if (cooldownMessage != null) {
            return cooldownMessage;
        }

        double increaseBonus = gameConfigService.getDouble(GameSetting.SELF_GROWTH_BONUS);
        double maxGrowthBonus = gameConfigService.getDouble(GameSetting.MAX_PENDING_GROWTH_BONUS);

        player.setLastAbilityTime(LocalDateTime.now(clock));
        player.setPendingGrowthBonus(Math.min(
            maxGrowthBonus,
            round(player.getPendingCritChanceBonus() + increaseBonus)
        ));

        playerDao.save(player);
        return "Вы усилили свой следующий рост на " + toPercent(increaseBonus) + "%.";
    }

    public String pray(Platform platform, long profileId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        String cooldownMessage = checkCooldown(player);
        if (cooldownMessage != null) {
            return cooldownMessage;
        }

        double increaseBonus = gameConfigService.getDouble(GameSetting.SELF_FAIL_BONUS);

        player.setLastAbilityTime(LocalDateTime.now(clock));
        player.setPendingFailChancePenalty(
            round(player.getPendingFailChancePenalty() - increaseBonus)
        );

        playerDao.save(player);
        return "Вы уменьшили шанс неудачи на " + toPercent(increaseBonus) + "% при следующем росте.";
    }


    private String validateTarget(Platform platform, long scopeId, Player actor, Player target) {
        if (actor.getId().equals(target.getId())) {
            return "Нельзя использовать эту способность на себе.";
        }

        if (!conversationParticipantRepository.existsByPlayerIdAndPlatformAndScopeId(target.getId(), platform, scopeId)) {
            return "Цель не найдена в этой беседе.";
        }

        return null;
    }

    private String checkCooldown(Player player) {
        LocalDateTime lastAbilityTime = player.getLastAbilityTime();
        if (lastAbilityTime == null) {
            return null;
        }

        int timeRange = gameConfigService.getInt(GameSetting.ABILITY_COOLDOWN_RANGE);
        ChronoUnit timeUnit = gameConfigService.getChronoUnit(GameSetting.ABILITY_COOLDOWN_UNIT);
        LocalDateTime nextAllowed = lastAbilityTime.plus(timeRange, timeUnit);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isBefore(nextAllowed)) {
            return null;
        }

        Duration duration = Duration.between(now, nextAllowed);
        return "Способность еще на перезарядке. Доступно через: " + formatDuration(duration);
    }

    private String formatDuration(Duration duration) {
        StringBuilder builder = new StringBuilder();
        if (duration.toHours() > 0) {
            builder.append(duration.toHours()).append(" ч ");
        }
        if (duration.toMinutesPart() > 0) {
            builder.append(duration.toMinutesPart()).append(" мин ");
        }
        builder.append(duration.toSecondsPart()).append(" сек");
        return builder.toString();
    }

    private int toPercent(double value) {
        return (int) Math.round(value * 100);
    }
}
