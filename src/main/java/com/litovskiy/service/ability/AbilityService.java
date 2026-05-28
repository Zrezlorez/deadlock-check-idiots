package com.litovskiy.service.ability;

import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.log.metadata.FuckLogMetadata;
import com.litovskiy.log.metadata.GrowthModifierLogMetadata;
import com.litovskiy.log.metadata.JackpotLogMetadata;
import com.litovskiy.log.metadata.PrayLogMetadata;
import com.litovskiy.log.metadata.TransferLogMetadata;
import com.litovskiy.repository.ConversationParticipantRepository;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.PlayerAccountService;
import com.litovskiy.service.data.GameConfigService;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.log.PlayerBehaviorStatService;
import com.litovskiy.service.log.GameLogService;
import com.litovskiy.util.CommandBlockReason;
import com.litovskiy.bot.CommandMessage;
import com.litovskiy.bot.CommandResult;
import com.litovskiy.service.data.GameSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.litovskiy.util.StringUtil.clamp;
import static com.litovskiy.util.StringUtil.formatDuration;
import static com.litovskiy.util.StringUtil.getKeyboard;
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
    private final PlayerBehaviorStatService playerBehaviorStatService;
    private final PlayerStatusService playerStatusService;
    private final Clock clock;


    public CommandResult fuck(Platform platform, long playerId, Long scopeId, long targetPlayerId) {
        if (scopeId == null) {
            return CommandResult.single("Эта способность доступна только в беседе или на сервере.");
        }

        AbilityTargetContext context = prepareTargetAbility(
            platform,
            playerId,
            scopeId,
            targetPlayerId
        );

        if (!context.success()) {
            return CommandResult.single(context.rejectionMessage());
        }


        Player actor = context.actor();
        Player target = context.target();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.FUCK);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        double costPercent = gameConfigService.getDouble(GameSetting.FUCK_COST_PERCENT);
        double failBonus = gameConfigService.getDouble(GameSetting.FUCK_FAIL_CHANCE_PENALTY);
        double maxFailChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_FAIL_CHANCE);

        double cost = round(actor.getSize() * costPercent);
        double oldActorSize = actor.getSize();

        if (actor.getSize() < target.getSize() * 0.25) {
            return CommandResult.single("Вы не можете атаковать игрока, если ваш рост меньше чем 25% от его размера");
        }

        actor.setSize(Math.max(1.0, round(actor.getSize() - cost)));
        actor.setLastAbilityTime(LocalDateTime.now(clock));

        double modifiedFailChance = playerStatusService.modifyFailChance(actor, target, failBonus);
        double oldTargetFailChance = target.getPendingFailChanceModifier();
        target.setPendingFailChanceModifier(Math.min(
            maxFailChance,
            round(oldTargetFailChance + modifiedFailChance)
        ));

        FuckLogMetadata logMetadata = new FuckLogMetadata(oldTargetFailChance, target.getPendingFailChanceModifier(), cost);

        gameLogService.logAbility(playerId, oldActorSize, actor.getSize(),
            targetPlayerId, target.getSize(), null,
            Action.FUCK, logMetadata);


        playerDao.save(target);
        playerDao.save(actor);

        String result = "Вы усилили шанс неудачи цели на " + toPercent(failBonus)
            + "% за " + GrowthStyle.convertValue(cost, growthStyle) + ". Следующая попытка роста у цели будет опаснее.";

        String statusMsg = "test";

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(statusMsg, getKeyboard())
        );
    }

    public CommandResult slow(Platform platform, long playerId, Long scopeId, long targetPlayerId) {
        if (scopeId == null) {
            return CommandResult.single("Эта способность доступна только в беседе или на сервере.");
        }


        AbilityTargetContext context = prepareTargetAbility(
            platform,
            playerId,
            scopeId,
            targetPlayerId
        );

        if (!context.success()) {
            return CommandResult.single(context.rejectionMessage());
        }

        Player actor = context.actor();
        Player target = context.target();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.SLOW);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        double growthPenalty = gameConfigService.getDouble(GameSetting.SLOW_GROWTH_PENALTY);
        double minGrowth = gameConfigService.getDouble(GameSetting.MIN_PENDING_GROWTH);
        double maxGrowth = gameConfigService.getDouble(GameSetting.MAX_PENDING_GROWTH);

        double affectedGrowthModifier = playerStatusService.modifyGrowthModifier(actor, target, Action.SLOW, growthPenalty);

        double oldTargetGrowthModifier = target.getPendingFailChanceModifier();
        double newTargetGrowthModifier = clamp(
            target.getPendingGrowthModifier() - affectedGrowthModifier,
            minGrowth,
            maxGrowth
        );
        actor.setLastAbilityTime(LocalDateTime.now(clock));
        target.setPendingGrowthModifier(newTargetGrowthModifier);

        GrowthModifierLogMetadata logMetadata = new GrowthModifierLogMetadata(oldTargetGrowthModifier, newTargetGrowthModifier, affectedGrowthModifier);
        gameLogService.logAbility(playerId, actor.getSize(), null,
            targetPlayerId, target.getSize(), null,
            Action.SLOW, logMetadata);

        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.SLOW);
        String statusMsg = playerStatusService.applyStatus(actor, stat);

        playerDao.save(target);
        playerDao.save(actor);

        String result = "Вы ослабили следующий рост цели на " + toPercent(affectedGrowthModifier) + "%.";

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(statusMsg)
        );
    }

    public CommandResult transfer(Platform platform, long playerId, Long scopeId, long targetPlayerId, String value) {
        if (scopeId == null) {
            return CommandResult.single("Эта способность доступна только в беседе или на сервере.");
        }

        double transferValue;
        try {
            transferValue = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return CommandResult.single("После команды нужно указать число для перевода");
        }

        AbilityTargetContext context = prepareTargetAbility(
            platform,
            playerId,
            scopeId,
            targetPlayerId
        );

        boolean ignoreCooldown = playerStatusService.ignoresAbilityCooldown(context.actor(), Action.TRANSFER);

        if (!ignoreCooldown && !context.success()) {
            return CommandResult.single(context.rejectionMessage());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Player actor = context.actor();
        Player target = context.target();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.TRANSFER);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        GrowthStyle growthStyle = conversationStyleService.getStyle(platform, scopeId);

        if (transferValue < 0 || !Double.isFinite(transferValue)) {
            double diff = actor.getSize() * 0.05;
            actor.setLastAbilityTime(now);
            actor.setSize(Math.max(
                1.0,
                round(actor.getSize() - diff)
            ));
            playerDao.save(actor);
            String result = "Вы обвиняетесь в подрыве государственного строя. " +
                "В качестве меры наказания к вам будет применен уголовный штраф в размере 5% от текущего роста. " +
                "Ваш рост уменьшен на " + GrowthStyle.convertValue(diff, growthStyle) + ".";
            return CommandResult.single(result);
        }

        if (actor.getSize() - 1 < transferValue) {
            return CommandResult.single("Вы не можете перевести больше, чем у вас есть");
        }

        String name = "камута";
        if (platform == Platform.TELEGRAM) {
            name = target.getTelegramDisplayName();
        } else if (platform == Platform.DISCORD) {
            name = target.getDiscordTag();
        }

        double costPercent = gameConfigService.getDouble(GameSetting.ABILITY_TRANSFER_COMMISSION);

        double affectedCostPercent = playerStatusService.modifyTransferCommission(actor, costPercent);
        double cost = round(transferValue * affectedCostPercent);

        double oldActorSize = actor.getSize();
        double oldTargetSize = target.getSize();

        actor.setSize(Math.max(1.0, round(actor.getSize() - transferValue)));
        if (!ignoreCooldown) {
            actor.setLastAbilityTime(now);
        }

        target.setSize(Math.max(
            1.0,
            round(target.getSize() + transferValue - affectedCostPercent)
        ));

        TransferLogMetadata logMetadata = new TransferLogMetadata(affectedCostPercent);

        gameLogService.logAbility(playerId, oldActorSize, actor.getSize(),
            targetPlayerId, oldTargetSize, target.getSize(),
            Action.TRANSFER, logMetadata);

        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.TRANSFER);
        String statusMsg = playerStatusService.applyStatus(actor, stat);

        playerDao.save(target);
        playerDao.save(actor);

        String result = "Вы перевели " + GrowthStyle.convertValue(transferValue, growthStyle) + " "
            + name + " с комиссией в размере " + GrowthStyle.convertValue(cost, growthStyle);

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(statusMsg)
        );
    }

    public CommandResult jackpot(Platform platform, long playerId) {
        AbilitySelfContext context = prepareSelfAbility(platform, playerId);

        if (!context.success()) {
            return CommandResult.single(context.rejectionMessage());
        }

        Player actor = context.player();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.JACKPOT);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        double failBonus = gameConfigService.getDouble(GameSetting.JACKPOT_FAIL_CHANCE);
        double critBonus = gameConfigService.getDouble(GameSetting.JACKPOT_CRIT_CHANCE);

        double maxCritChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_CRIT_CHANCE);
        double maxFailChance = gameConfigService.getDouble(GameSetting.MAX_PENDING_FAIL_CHANCE);

        double oldCritChance = actor.getPendingCritChanceModifier();
        double newCritChance = round(oldCritChance + critBonus);

        double oldFailChance = actor.getPendingFailChanceModifier();
        double newFailChance = round(oldFailChance + failBonus);

        actor.setLastAbilityTime(LocalDateTime.now(clock));
        actor.setPendingCritChanceModifier(Math.min(
            maxCritChance,
            newCritChance
        ));

        actor.setPendingFailChanceModifier(Math.min(
            maxFailChance,
            newFailChance
        ));

        JackpotLogMetadata logMetadata = new JackpotLogMetadata(
            oldCritChance, newCritChance,
            oldFailChance, newFailChance,
            newCritChance - oldCritChance,
            newFailChance - oldFailChance);

        gameLogService.logAbility(playerId, actor.getSize(), null,
            null, null, null,
            Action.JACKPOT, logMetadata);

        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.JACKPOT);
        String statusMsg = playerStatusService.applyStatus(actor, stat);

        playerDao.save(actor);
        String result = "Вы увеличили шанс джекпота на " + toPercent(critBonus) + "% и повысили шанс неудачи на " + toPercent(failBonus)
            + "% для следующего роста.";

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(statusMsg)
        );
    }

    public CommandResult turtle(Platform platform, long playerId) {
        AbilitySelfContext context = prepareSelfAbility(platform, playerId);

        if (!context.success()) {
            return CommandResult.single(context.rejectionMessage());
        }

        Player actor = context.player();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.TURTLE);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        double increaseBonus = gameConfigService.getDouble(GameSetting.TURTLE_GROWTH_BONUS);
        double maxGrowthBonus = gameConfigService.getDouble(GameSetting.MAX_PENDING_GROWTH);

        double affectedGrowthModifier = playerStatusService.modifyGrowthModifier(actor, null, Action.TURTLE, increaseBonus);

        double oldGrowthBonus = actor.getPendingGrowthModifier();


        actor.setLastAbilityTime(LocalDateTime.now(clock));
        actor.setPendingGrowthModifier(Math.min(
            maxGrowthBonus,
            round(oldGrowthBonus + affectedGrowthModifier)
        ));

        GrowthModifierLogMetadata logMetadata = new GrowthModifierLogMetadata(
            oldGrowthBonus,
            actor.getPendingGrowthModifier(),
            affectedGrowthModifier
        );

        gameLogService.logAbility(playerId, actor.getSize(), null,
            null, null, null,
            Action.TURTLE, logMetadata);


        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.TURTLE);
        String statusMsg = playerStatusService.applyStatus(actor, stat);

        playerDao.save(actor);
        String result = "Вы усилили свой следующий рост на " + toPercent(affectedGrowthModifier) + "%.";

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(statusMsg)
        );
    }

    public CommandResult pray(Platform platform, long playerId) {
        AbilitySelfContext context = prepareSelfAbility(platform, playerId);

        if (!context.success()) {
            return CommandResult.single(context.rejectionMessage());
        }

        Player actor = context.player();

        CommandBlockReason blockReason = playerStatusService.validateActionAllowed(actor, Action.PRAY);

        if (!blockReason.allowed()) {
            return CommandResult.single(blockReason.message());
        }

        double increaseBonus = gameConfigService.getDouble(GameSetting.PRAY_FAIL_BONUS);

        double oldFailChance = actor.getPendingFailChanceModifier();
        actor.setLastAbilityTime(LocalDateTime.now(clock));
        actor.setPendingFailChanceModifier(
            round(oldFailChance - increaseBonus)
        );

        PrayLogMetadata logMetadata = new PrayLogMetadata(
            oldFailChance,
            actor.getPendingFailChanceModifier(),
            increaseBonus
        );
        gameLogService.logAbility(playerId, actor.getSize(), null,
            null, null, null,
            Action.PRAY, logMetadata);

        PlayerBehaviorStats stat = playerBehaviorStatService.applyAbility(playerId, Action.TURTLE);
        String statusMsg = playerStatusService.applyStatus(actor, stat);

        playerDao.save(actor);
        String result = "Вы уменьшили шанс неудачи на " + toPercent(increaseBonus) + "% при следующем росте.";

        return CommandResult.of(
            CommandMessage.reply(result),
            CommandMessage.broadcast(statusMsg)
        );
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
        Player target = playerAccountService.resolveOrCreate(platform, targetProfileId);

        if (cooldownMessage != null) {
            return AbilityTargetContext.rejected(now, actor, target, cooldownMessage);
        }

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
