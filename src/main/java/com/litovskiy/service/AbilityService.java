package com.litovskiy.service;

import com.litovskiy.log.Action;
import com.litovskiy.log.metadata.FuckLogMetadata;
import com.litovskiy.log.metadata.GrowthModifierLogMetadata;
import com.litovskiy.log.metadata.JackpotLogMetadata;
import com.litovskiy.log.metadata.PrayLogMetadata;
import com.litovskiy.log.metadata.TransferLogMetadata;
import com.litovskiy.repository.ConversationParticipantRepository;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.util.AbilitySelfContext;
import com.litovskiy.util.AbilityTargetContext;
import com.litovskiy.service.log.GameLogService;
import com.litovskiy.util.GameSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.litovskiy.util.StringUtil.clamp;
import static com.litovskiy.util.StringUtil.formatDuration;
import static com.litovskiy.util.StringUtil.round;

@Component
@RequiredArgsConstructor
public class AbilityService {

    private final PlayerService playerDao;
    private final PlayerAccountService playerAccountService;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final GameConfigService gameConfigService;
    private final GameLogService gameLogService;
    private final ConversationStyleService conversationStyleService;
    private final Clock clock;


    public String fuck(Platform platform, long playerId, Long scopeId, long targetPlayerId) {
        if (scopeId == null) {
            return "Эта способность доступна только в беседе или на сервере.";
        }

        AbilityTargetContext context = prepareTargetAbility(
            platform,
            playerId,
            scopeId,
            targetPlayerId
        );

        if (!context.success()) {
            return context.rejectionMessage();
        }

        Player actor = context.actor();
        Player target = context.target();

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        double costPercent = gameConfigService.getDouble(GameSetting.FUCK_COST_PERCENT);
        double failBonus = gameConfigService.getDouble(GameSetting.FUCK_FAIL_CHANCE_PENALTY);
        double maxFailChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_FAIL_CHANCE);

        double cost = round(actor.getSize() * costPercent);
        double oldActorSize = actor.getSize();

        actor.setSize(Math.max(1.0, round(actor.getSize() - cost)));
        actor.setLastAbilityTime(LocalDateTime.now(clock));

        double oldTargetFailChance = target.getPendingFailChanceModifier();

        target.setPendingFailChanceModifier(Math.min(
            maxFailChance,
            round(oldTargetFailChance + failBonus)
        ));


        FuckLogMetadata logMetadata = new FuckLogMetadata(oldTargetFailChance, target.getPendingFailChanceModifier(), cost);
        gameLogService.logAbility(playerId, oldActorSize, actor.getSize(),
            targetPlayerId, target.getSize(), null,
            Action.FUCK, logMetadata);

        playerDao.save(target);
        playerDao.save(actor);

        return "Вы усилили шанс неудачи цели на " + toPercent(failBonus)
            + "% за " + GrowthStyle.convertValue(cost, growthStyle) + ". Следующая попытка роста у цели будет опаснее.";
    }

    public String slow(Platform platform, long playerId, Long scopeId, long targetPlayerId) {
        if (scopeId == null) {
            return "Эта способность доступна только в беседе или на сервере.";
        }


        AbilityTargetContext context = prepareTargetAbility(
            platform,
            playerId,
            scopeId,
            targetPlayerId
        );

        if (!context.success()) {
            return context.rejectionMessage();
        }

        Player actor = context.actor();
        Player target = context.target();

        double growthPenalty = gameConfigService.getDouble(GameSetting.SLOW_GROWTH_PENALTY);
        double minGrowth = gameConfigService.getDouble(GameSetting.MIN_PENDING_GROWTH);
        double maxGrowth = gameConfigService.getDouble(GameSetting.MAX_PENDING_GROWTH);

        double oldTargetGrowthModifier = target.getPendingFailChanceModifier();
        double newTargetGrowthModifier = clamp(
            target.getPendingGrowthModifier() - growthPenalty,
            minGrowth,
            maxGrowth
        );
        actor.setLastAbilityTime(LocalDateTime.now(clock));
        target.setPendingGrowthModifier(newTargetGrowthModifier);

        GrowthModifierLogMetadata logMetadata = new GrowthModifierLogMetadata(oldTargetGrowthModifier, newTargetGrowthModifier, growthPenalty);
        gameLogService.logAbility(playerId, actor.getSize(), null,
            targetPlayerId, target.getSize(), null,
            Action.SLOW, logMetadata);

        playerDao.save(target);
        playerDao.save(actor);

        return "Вы ослабили следующий рост цели на " + toPercent(growthPenalty) + "%.";
    }

    public String transfer(Platform platform, long playerId, Long scopeId, long targetPlayerId, String value) {
        if (scopeId == null) {
            return "Эта способность доступна только в беседе или на сервере.";
        }

        double transferValue;
        try {
            transferValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return "После команды нужно указать число для перевода";
        }

        AbilityTargetContext context = prepareTargetAbility(
            platform,
            playerId,
            scopeId,
            targetPlayerId
        );

        if (!context.success()) {
            return context.rejectionMessage();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Player actor = context.actor();
        Player target = context.target();

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        if (transferValue < 0 || !Double.isFinite(transferValue)) {
            double diff = actor.getSize() * 0.05;
            actor.setLastAbilityTime(now);
            actor.setSize(Math.max(
                1.0,
                round(actor.getSize() - diff)
            ));
            playerDao.save(actor);
            return "Вы обвиняетесь в подрыве государственного строя. " +
                "В качестве меры наказания к вам будет применен уголовный штраф в размере 5% от текущего роста. " +
                "Ваш рост уменьшен на " + GrowthStyle.convertValue(diff, growthStyle) + ".";
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

        double oldActorSize = actor.getSize();
        double oldTargetSize = target.getSize();

        actor.setSize(Math.max(1.0, round(actor.getSize() - transferValue)));
        actor.setLastAbilityTime(now);

        target.setSize(Math.max(
            1.0,
            round(target.getSize() + transferValue - cost)
        ));

        TransferLogMetadata logMetadata = new TransferLogMetadata(cost);
        gameLogService.logAbility(playerId, oldActorSize, actor.getSize(),
            targetPlayerId, oldTargetSize, target.getSize(),
            Action.TRANSFER, logMetadata);

        playerDao.save(target);
        playerDao.save(actor);
        return "Вы перевели " + GrowthStyle.convertValue(transferValue, growthStyle) + " "
            + name + " с комиссией в размере " + GrowthStyle.convertValue(cost, growthStyle);
    }

    public String jackpot(Platform platform, long playerId) {
        AbilitySelfContext context = prepareSelfAbility(platform, playerId);

        if (!context.success()) {
            return context.rejectionMessage();
        }

        Player player = context.player();

        double failBonus = gameConfigService.getDouble(GameSetting.JACKPOT_FAIL_CHANCE);
        double critBonus = gameConfigService.getDouble(GameSetting.JACKPOT_CRIT_CHANCE);

        double maxCritChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_CRIT_CHANCE);
        double maxFailChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_FAIL_CHANCE);

        double oldCritChance = player.getPendingCritChanceModifier();
        double newCritChance = round(oldCritChance + critBonus);

        double oldFailChance = player.getPendingFailChanceModifier();
        double newFailChance = round(oldFailChance + failBonus);

        player.setLastAbilityTime(LocalDateTime.now(clock));
        player.setPendingCritChanceModifier(Math.min(
            maxCritChance,
            newCritChance
        ));

        player.setPendingFailChanceModifier(Math.min(
            maxFailChance,
            newFailChance
        ));

        JackpotLogMetadata logMetadata = new JackpotLogMetadata(
            oldCritChance, newCritChance,
            oldFailChance, newFailChance,
            newCritChance - oldCritChance,
            newFailChance - oldFailChance);

        gameLogService.logAbility(playerId, player.getSize(), null,
            null, null, null,
            Action.JACKPOT, logMetadata);

        playerDao.save(player);
        return "Вы увеличили шанс джекпота на " + toPercent(critBonus) + "% и повысили шанс неудачи на " + toPercent(failBonus)
            + "% для следующего роста.";
    }

    public String turtle(Platform platform, long playerId) {
        AbilitySelfContext context = prepareSelfAbility(platform, playerId);

        if (!context.success()) {
            return context.rejectionMessage();
        }

        Player player = context.player();

        double increaseBonus = gameConfigService.getDouble(GameSetting.TURTLE_GROWTH_BONUS);
        double maxGrowthBonus = gameConfigService.getDouble(GameSetting.MAX_PENDING_GROWTH);

        double oldGrowthBonus = player.getPendingGrowthModifier();

        player.setLastAbilityTime(LocalDateTime.now(clock));
        player.setPendingGrowthModifier(Math.min(
            maxGrowthBonus,
            round(oldGrowthBonus + increaseBonus)
        ));

        GrowthModifierLogMetadata logMetadata = new GrowthModifierLogMetadata(
            oldGrowthBonus,
            player.getPendingGrowthModifier(),
            increaseBonus
        );

        gameLogService.logAbility(playerId, player.getSize(), null,
            null, null, null,
            Action.TURTLE, logMetadata);


        playerDao.save(player);
        return "Вы усилили свой следующий рост на " + toPercent(increaseBonus) + "%.";
    }

    public String pray(Platform platform, long profileId) {
        AbilitySelfContext context = prepareSelfAbility(platform, profileId);

        if (!context.success()) {
            return context.rejectionMessage();
        }

        Player player = context.player();

        double increaseBonus = gameConfigService.getDouble(GameSetting.PRAY_FAIL_BONUS);

        double oldFailChance = player.getPendingFailChanceModifier();
        player.setLastAbilityTime(LocalDateTime.now(clock));
        player.setPendingFailChanceModifier(
            round(oldFailChance - increaseBonus)
        );

        PrayLogMetadata logMetadata = new PrayLogMetadata(
            oldFailChance,
            player.getPendingFailChanceModifier(),
            increaseBonus
        );
        gameLogService.logAbility(profileId, player.getSize(), null,
            null, null, null,
            Action.PRAY, logMetadata);

        playerDao.save(player);
        return "Вы уменьшили шанс неудачи на " + toPercent(increaseBonus) + "% при следующем росте.";
    }

    private AbilitySelfContext prepareSelfAbility(Platform platform, long profileId) {
        LocalDateTime now = LocalDateTime.now(clock);

        Player player = playerAccountService.resolveOrCreate(platform, profileId);

        String cooldownMessage = checkCooldown(player, now);
        if (cooldownMessage != null) {
            return AbilitySelfContext.rejected(cooldownMessage);
        }

        return AbilitySelfContext.success(now, player);
    }

    private AbilityTargetContext prepareTargetAbility(Platform platform, long profileId,
                                                      Long scopeId, long targetProfileId)
    {
        LocalDateTime now = LocalDateTime.now(clock);

        Player actor = playerAccountService.resolveOrCreate(platform, profileId);

        String cooldownMessage = checkCooldown(actor, now);
        if (cooldownMessage != null) {
            return AbilityTargetContext.rejected(cooldownMessage);
        }

        Player target = playerAccountService.resolveOrCreate(platform, targetProfileId);

        String validationMessage = validateTarget(platform, scopeId, actor, target);
        if (validationMessage != null) {
            return AbilityTargetContext.rejected(validationMessage);
        }

        return AbilityTargetContext.success(now, actor, target);
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

    private String checkCooldown(Player player, LocalDateTime now) {
        LocalDateTime lastAbilityTime = player.getLastAbilityTime();
        if (lastAbilityTime == null) {
            return null;
        }

        int timeRange = gameConfigService.getInt(GameSetting.ABILITY_COOLDOWN_RANGE);
        ChronoUnit timeUnit = gameConfigService.getChronoUnit(GameSetting.ABILITY_COOLDOWN_UNIT);
        LocalDateTime nextAllowed = lastAbilityTime.plus(timeRange, timeUnit);
        if (!now.isBefore(nextAllowed)) {
            return null;
        }

        Duration duration = Duration.between(now, nextAllowed);
        return "Способность еще на перезарядке. Доступно через: " + formatDuration(duration);
    }

    private int toPercent(double value) {
        return (int) Math.round(value * 100);
    }
}
