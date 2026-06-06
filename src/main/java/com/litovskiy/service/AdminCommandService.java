package com.litovskiy.service;

import com.litovskiy.config.cloud.GithubConfigService;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.data.PlayerService;
import com.litovskiy.util.CommandResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class AdminCommandService {

    private final AdminAccessService adminAccessService;
    private final PlayerService playerDao;
    private final DateTimeFormatter formatter;
    private final GithubConfigService githubConfigService;

    public CommandResult handle(Platform requesterPlatform, long requesterProfileId, String rawCommand) {
        if (!adminAccessService.isAdmin(requesterPlatform, requesterProfileId)) {
            return reply("Нет доступа к админке. " + adminAccessService.describeConfiguration());
        }

        String commandBody = rawCommand == null ? "" : rawCommand.trim();
        if (commandBody.isEmpty()) {
            return reply(help());
        }

        String[] parts = commandBody.split("\\s+");
        return switch (parts[0].toLowerCase()) {
            case "config" -> reply("Временно отключено");
            case "reload-config" -> reply(githubConfigService.reload().message());
            case "player" -> reply(handlePlayer(parts));
            case "help" -> reply(help());
            default -> reply("Неизвестная admin-команда.\n" + help());
        };
    }

    private CommandResult reply(String text) {
        return CommandResult.single(text);
    }

    private String handlePlayer(String[] parts) {
        if (parts.length < 4) {
            return "Использование: admin player <command> <platform> <username|tag|id> [values]";
        }

        Platform targetPlatform = parsePlatform(parts[2]);
        if (targetPlatform == null) {
            return "Неизвестная платформа. Используйте telegram или discord.";
        }

        String identifier = parts[3].toLowerCase();
        Player player = resolvePlayer(targetPlatform, identifier);
        if (player == null) {
            return "Игрок не найден.";
        }

        return switch (parts[1].toLowerCase()) {
            case "show" -> formatPlayer(targetPlatform, identifier, player);
            case "set-size" -> updatePlayerSize(player, parts, true);
            case "add-size" -> updatePlayerSize(player, parts, false);
            case "reset-cooldown" -> resetCooldown(player);
            case "log" -> getLogs(player, parts);
            case "set-last-grow" -> setLastGrow(player, parts);
            case "reset-ability" -> resetAbilityCooldown(player);
            case "set-last-ability" -> setLastAbility(player, parts);
            default -> "Неизвестная команда игрока.";
        };
    }

    // TODO: доделать логи в админке
    private String getLogs(Player player, String[] parts) {
        return "В разработке";
    }

    private Player resolvePlayer(Platform platform, String identifier) {
        Player player = switch (platform) {
            case TELEGRAM -> playerDao.findByTelegramUsername(identifier);
            case DISCORD -> playerDao.findByDiscordTag(identifier);
        };
        if (player != null) {
            return player;
        }

        try {
            return playerDao.findByPlatform(platform, Long.parseLong(identifier));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String updatePlayerSize(Player player, String[] parts, boolean replace) {
        if (parts.length < 5) {
            return "Нужно указать значение размера.";
        }

        try {
            double value = Double.parseDouble(parts[4]);
            player.setSize(replace ? value : player.getSize() + value);
            playerDao.save(player);
            return "Размер игрока обновлен: " + player.getSize();
        } catch (NumberFormatException e) {
            return "Размер должен быть числом.";
        }
    }

    private String resetCooldown(Player player) {
        player.setLastGrowTime(null);
        playerDao.save(player);
        return "Кулдаун игрока сброшен.";
    }

    private String resetAbilityCooldown(Player player) {
        player.setLastAbilityTime(null);
        playerDao.save(player);
        return "Кулдаун способностей игрока сброшен.";
    }

    private String setLastGrow(Player player, String[] parts) {
        if (parts.length < 5) {
            return "Нужно указать время в формате yyyy-MM-ddTHH:mm:ss или none.";
        }

        if ("none".equalsIgnoreCase(parts[4])) {
            player.setLastGrowTime(null);
            playerDao.save(player);
            return "lastGrowTime очищен.";
        }

        try {
            player.setLastGrowTime(LocalDateTime.parse(parts[4]));
            playerDao.save(player);
            return "lastGrowTime обновлен: " + player.getLastGrowTime();
        } catch (DateTimeParseException e) {
            return "Неверный формат времени. Используйте yyyy-MM-ddTHH:mm:ss";
        }
    }

    private String setLastAbility(Player player, String[] parts) {
        if (parts.length < 5) {
            return "Нужно указать время в формате yyyy-MM-ddTHH:mm:ss или none.";
        }

        if ("none".equalsIgnoreCase(parts[4])) {
            player.setLastAbilityTime(null);
            playerDao.save(player);
            return "lastGrowTime очищен.";
        }

        try {
            player.setLastAbilityTime(LocalDateTime.parse(parts[4]));
            playerDao.save(player);
            return "lastGrowTime обновлен: " + player.getLastGrowTime();
        } catch (DateTimeParseException e) {
            return "Неверный формат времени. Используйте yyyy-MM-ddTHH:mm:ss";
        }
    }

    private String formatPlayer(Platform platform, String identifier, Player player) {
        return "Игрок найден:\n"
            + "platform = " + platform.displayName() + "\n"
            + "identifier = " + identifier + "\n"
            + "playerChatId = " + player.getId() + "\n"
            + "size = " + player.getSize() + "\n"
            + "lastGrowTime = " + player.getLastGrowTime().format(formatter) + "\n"
            + "lastGrowTime = " + player.getLastAbilityTime().format(formatter) + "\n"
            + "telegramChatId = " + player.getTelegramChatId() + "\n"
            + "telegramUsername = " + player.getTelegramUsername() + "\n"
            + "discordUserId = " + player.getDiscordUserId() + "\n"
            + "discordTag = " + player.getDiscordTag();
    }

    private Platform parsePlatform(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        return switch (rawValue.trim().toLowerCase()) {
            case "telegram", "tg" -> Platform.TELEGRAM;
            case "discord", "ds" -> Platform.DISCORD;
            default -> null;
        };
    }

    private String help() {
        return """
            Admin-команды:
            admin config
            admin set <key> <value>
            admin reset <key>
            admin reload-config
            admin player show <platform> <username|tag|id>
            admin player set-size <platform> <username|tag|id> <value>
            admin player add-size <platform> <username|tag|id> <value>
            admin player reset-cooldown <platform> <username|tag|id>
            admin player set-last-grow <platform> <username|tag|id> <yyyy-MM-ddTHH:mm:ss|none>
            """.trim();
    }
}
