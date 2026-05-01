package com.litovskiy.service;

import com.litovskiy.dao.ConversationParticipantDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.ConversationSettings;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.litovskiy.util.StringUtil.round;

public class AbilityService {

    private static final double MAX_PENDING_FAIL_CHANCE_BONUS = 0.45;
    private static final double MAX_PENDING_CRIT_CHANCE_BONUS = 0.45;
    private static final double MAX_PENDING_GROWTH_PENALTY = 0.45;

    private final PlayerDao playerDao;
    private final PlayerAccountService playerAccountService;
    private final ConversationParticipantDao conversationParticipantDao;
    private final GameConfigService gameConfigService;
    private final ConversationStyleService conversationStyleService;
    private final Clock clock;

    public AbilityService(PlayerDao playerDao,
                          PlayerAccountService playerAccountService,
                          ConversationParticipantDao conversationParticipantDao,
                          GameConfigService gameConfigService, ConversationStyleService conversationStyleService) {
        this(playerDao, playerAccountService, conversationParticipantDao, gameConfigService, conversationStyleService, Clock.systemDefaultZone());
    }

    public AbilityService(PlayerDao playerDao,
                          PlayerAccountService playerAccountService,
                          ConversationParticipantDao conversationParticipantDao,
                          GameConfigService gameConfigService, ConversationStyleService conversationStyleService,
                          Clock clock) {
        this.playerDao = playerDao;
        this.playerAccountService = playerAccountService;
        this.conversationParticipantDao = conversationParticipantDao;
        this.gameConfigService = gameConfigService;
        this.conversationStyleService = conversationStyleService;
        this.clock = clock;
    }

    public String increaseEnemyFailChance(Platform platform, long profileId, Long scopeId, long targetProfileId) {
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
        double failBonus = gameConfigService.getDouble(GameSetting.ENEMY_FAIL_CHANCE_BONUS);
        double cost = round(actor.getSize() * costPercent);

        actor.setSize(Math.max(0.0, round(actor.getSize() - cost)));
        actor.setLastAbilityTime(LocalDateTime.now(clock));
        target.setPendingFailChanceBonus(Math.min(
            MAX_PENDING_FAIL_CHANCE_BONUS,
            round(target.getPendingFailChanceBonus() + failBonus)
        ));

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        playerDao.save(target);
        playerDao.save(actor);
        return "Вы усилили шанс неудачи цели на " + toPercent(failBonus)
            + " % за " + GrowthStyle.convertValue(cost, growthStyle) + ". Следующая попытка роста у цели будет опаснее.";
    }

    public String increaseOwnCritChance(Platform platform, long scopeId, long profileId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        String cooldownMessage = checkCooldown(player);
        if (cooldownMessage != null) {
            return cooldownMessage;
        }

        double costPercent = gameConfigService.getDouble(GameSetting.SELF_CRIT_COST_PERCENT);
        double critBonus = gameConfigService.getDouble(GameSetting.SELF_CRIT_CHANCE_BONUS);
        double cost = round(player.getSize() * costPercent);

        player.setSize(Math.max(0.0, round(player.getSize() - cost)));
        player.setLastAbilityTime(LocalDateTime.now(clock));
        player.setPendingCritChanceBonus(Math.min(
            MAX_PENDING_CRIT_CHANCE_BONUS,
            round(player.getPendingCritChanceBonus() + critBonus)
        ));


        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        playerDao.save(player);
        return "Вы пожертвовали " + GrowthStyle.convertValue(cost, growthStyle) + " и повысили шанс джекпота на " + toPercent(critBonus)
            + "% для следующего роста.";
    }

    public String reduceEnemyGrowth(Platform platform, long profileId, Long scopeId, long targetProfileId) {
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
        actor.setLastAbilityTime(LocalDateTime.now(clock));
        target.setPendingGrowthPenalty(Math.min(
            MAX_PENDING_GROWTH_PENALTY,
            round(target.getPendingGrowthPenalty() + growthPenalty)
        ));

        playerDao.save(target);
        playerDao.save(actor);
        return "Вы ослабили следующий рост цели на " + toPercent(growthPenalty) + " %.";
    }

    private String validateTarget(Platform platform, long scopeId, Player actor, Player target) {
        if (actor.getChatId().equals(target.getChatId())) {
            return "Нельзя использовать эту способность на себе.";
        }

        if (!conversationParticipantDao.exists(target.getChatId(), platform, scopeId)) {
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
