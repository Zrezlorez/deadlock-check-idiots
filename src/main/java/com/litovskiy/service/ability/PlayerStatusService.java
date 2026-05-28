package com.litovskiy.service.ability;

import com.litovskiy.entity.Player;
import com.litovskiy.entity.PlayerBehaviorStats;
import com.litovskiy.log.Action;
import com.litovskiy.util.CommandBlockReason;
import com.litovskiy.util.GrowthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlayerStatusService {
    //TODO: вынести все константы в кфг

    private final Clock clock;

    public CommandBlockReason validateActionAllowed(Player player, Action action) {
        PlayerStatus status = getActiveStatus(player);

        if (status == PlayerStatus.NONE) {
            return CommandBlockReason.createAllowed();
        }

        if (status == PlayerStatus.LUDOMANIA) {
            return  switch (action) {
                case JACKPOT, TURTLE ->
                    CommandBlockReason.createBlocked(
                        "Ваша лудомания не позволяет вам использовать команды, вы хотите только крутить казик. До окончания болезни /turtle и /jackpot временно недоступны."
                    );
                default -> CommandBlockReason.createAllowed();
            };
        }
        if (status == PlayerStatus.SHAO_LIN) {
            return switch (action) {
                case FUCK, SLOW ->
                    CommandBlockReason.createBlocked(
                        "Вы вступили в ряды шаолиньских монахов. Агрессия для вас недопустима."
                    );
                default -> CommandBlockReason.createAllowed();
            };
        }

        return CommandBlockReason.createAllowed();
    }

    /**
     * Нужно ли игнорировать способность.
     * Используется для статуса TRADER со способностью TRANSFER
     */
    public boolean ignoresAbilityCooldown(Player player, Action action) {
        PlayerStatus status = getActiveStatus(player);

        return status == PlayerStatus.TRADER && action == Action.TRANSFER;
    }

    /**
     * Получить измененную комиссию.
     * Используется для статуса TRADER
     */
    public double modifyTransferCommission(Player actor, double commission) {
        return getActiveStatus(actor) == PlayerStatus.TRADER ? 0.0 : commission;
    }

    /**
     * Изменить модификатор доп. роста
     *      target PRIEST = при SLOW return 0
     *      actor CRUD = при SLOW return value x1.5
     *      actor SHAO_LIN = при TURTLE return value 0.5
     */
    public double modifyGrowthModifier(Player actor, Player target, Action action, double value) {
        PlayerStatus actorStatus = getActiveStatus(actor);
        PlayerStatus targetStatus = getActiveStatus(target);

        if (action == Action.SLOW) {
            if (targetStatus == PlayerStatus.PRIEST) {
                return 0;
            }

            if (actorStatus == PlayerStatus.CRUD) {
                return value * 2;
            }
        }

        if (action == Action.TURTLE) {
            if (actorStatus == PlayerStatus.SHAO_LIN) {
                return 0.5;
            }
        }

        return value;
    }

    /**
     * Изменить модификатор джекпота.
     *      target PRIEST = при SLOW return 0
     *      actor CRUD = при SLOW return value x2
     */
    public double modifyFailChance(Player actor, Player target, double value) {
        PlayerStatus actorStatus = getActiveStatus(actor);
        PlayerStatus targetStatus = getActiveStatus(target);

        if (targetStatus == PlayerStatus.PRIEST) {
            return 0;
        }

        if (actorStatus == PlayerStatus.CRUD) {
            return value * 2;
        }

        return value;
    }

    /**
     * Изменить шансы джекпота и неудачи при росте.
     * При LUDOMANIA
     *      fail = 0.05
     *      crit = 0.05
     *      critMultiplier += 45
     *      failPercent += 0.4
     * При SHAO_LIN
     *      fail - 0.1
     *      crit - 0.1
     */
    public GrowthContext applyGrowthStatusEffects(Player player, GrowthContext context) {
        return switch (getActiveStatus(player)) {
            case LUDOMANIA -> context
                .withFailChanceModifier(0.05)
                .withCritChanceModifier(0.05)
                .withCritMultiplier(context.critMultiplier() + 45)
                .withFailPercent(0.4);
            case SHAO_LIN -> context
                .withFailChanceModifier(context.failChance() - 0.10)
                .withCritChanceModifier(context.critChance() - 0.10);
            case CRUD -> context
                .withGrowthModifier(-0.25);
            default -> context;
        };
    }


    public String applyStatus(Player player, PlayerBehaviorStats stat) {
        if (stat.getSameAbilityStreak() < 3) {
            return null;
        }

        Action lastAction = stat.getLastAbilityAction();
        if (lastAction == null) {
            return null;
        }

        String message = apply(player, lastAction.getEvent());

        if (message != null) {
            stat.setLastAbilityAction(null);
            stat.setSameAbilityStreak(0);
        }

        return message;
    }

    public PlayerStatus getActiveStatus(Player player) {
        if (player == null) {
            return PlayerStatus.NONE;
        }

        if (player.getStatus() == null || player.getStatus() == PlayerStatus.NONE) {
            return PlayerStatus.NONE;
        }

        if (player.getStatusUntil() == null) {
            return PlayerStatus.NONE;
        }

        if (!player.getStatusUntil().isAfter(LocalDateTime.now(clock))) {
            return PlayerStatus.NONE;
        }

        return player.getStatus();
    }

    private String apply(Player player, PlayerStatus status) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (status == PlayerStatus.NONE) {
            return null;
        }

        if (hasActiveStatus(player, now)) {
            return null;
        }

        player.setStatus(status);
        player.setStatusUntil(now.plusHours(36));

        return status.getMessage();
    }

    private boolean hasActiveStatus(Player player, LocalDateTime now) {
        return player.getStatus() != null
            && player.getStatus() != PlayerStatus.NONE
            && player.getStatusUntil() != null
            && player.getStatusUntil().isAfter(now);
    }
}
