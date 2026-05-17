package com.litovskiy.service;

import com.litovskiy.service.data.PlayerService;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminCommandService {

    private final AdminAccessService adminAccessService;
    private final GameConfigService gameConfigService;
    private final PlayerService playerDao;

    public String handle(Platform requesterPlatform, long requesterProfileId, String rawCommand) {
        if (!adminAccessService.isAdmin(requesterPlatform, requesterProfileId)) {
            return "Нет доступа к админке. " + adminAccessService.describeConfiguration();
        }

        String commandBody = rawCommand == null ? "" : rawCommand.trim();
        if (commandBody.isEmpty()) {
            return help();
        }

        String[] parts = commandBody.split("\\s+");
        return switch (parts[0].toLowerCase()) {
            case "config" -> showConfig();
            case "set" -> setConfig(parts);
            case "reset" -> resetConfig(parts);
            case "player" -> handlePlayer(parts);
            case "help" -> help();
            default -> "Неизвестная admin-команда.\n" + help();
        };
    }

    private String showConfig() {
        StringBuilder builder = new StringBuilder("Текущие настройки:\n");
        for (Map.Entry<GameSetting, String> entry : gameConfigService.listEffectiveValues().entrySet()) {
            builder.append(entry.getKey().getKey())
                .append(" = ")
                .append(entry.getValue())
                .append(" | ")
                .append(entry.getKey().getDescription())
                .append('\n');
        }
        return builder.toString().trim();
    }

    private String setConfig(String[] parts) {
        if (parts.length < 3) {
            return "Использование: admin set <key> <value>";
        }

        GameSetting setting = GameSetting.fromKey(parts[1]);
        if (setting == null) {
            return "Неизвестная настройка. Доступны: " + availableSettings();
        }

        try {
            gameConfigService.set(setting, parts[2]);
            return "Настройка обновлена: " + setting.getKey() + " = " + gameConfigService.getRawValue(setting);
        } catch (RuntimeException e) {
            return "Не удалось сохранить настройку: " + e.getMessage();
        }
    }

    private String resetConfig(String[] parts) {
        if (parts.length < 2) {
            return "Использование: admin reset <key>";
        }

        GameSetting setting = GameSetting.fromKey(parts[1]);
        if (setting == null) {
            return "Неизвестная настройка. Доступны: " + availableSettings();
        }

        gameConfigService.reset(setting);
        return "Настройка сброшена к дефолту: " + setting.getKey() + " = " + setting.getDefaultValue();
    }

    private String handlePlayer(String[] parts) {
        if (parts.length < 4) {
            return "Использование: admin player <show|set-size|add-size|reset-cooldown|set-last-grow> <platform> <username|tag|id> [value]";
        }

        Platform targetPlatform = parsePlatform(parts[2]);
        if (targetPlatform == null) {
            return "Неизвестная платформа. Используйте telegram или discord.";
        }

        String identifier = parts[3];
        Player player = resolvePlayer(targetPlatform, identifier);
        if (player == null) {
            return "Игрок не найден.";
        }

        return switch (parts[1].toLowerCase()) {
            case "show" -> formatPlayer(targetPlatform, identifier, player);
            case "set-size" -> updatePlayerSize(player, parts, true);
            case "add-size" -> updatePlayerSize(player, parts, false);
            case "reset-cooldown" -> resetCooldown(player);
            case "set-last-grow" -> setLastGrow(player, parts);
            default -> "Неизвестная команда игрока.";
        };
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

    private String formatPlayer(Platform platform, String identifier, Player player) {
        return "Игрок найден:\n"
            + "platform = " + platform.displayName() + "\n"
            + "identifier = " + identifier + "\n"
            + "playerChatId = " + player.getId() + "\n"
            + "size = " + player.getSize() + "\n"
            + "lastGrowTime = " + player.getLastGrowTime() + "\n"
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

    private String availableSettings() {
        return Arrays.stream(GameSetting.values())
            .map(GameSetting::getKey)
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }

    private String help() {
        return """
            Admin-команды:
            admin config
            admin set <key> <value>
            admin reset <key>
            admin player show <platform> <username|tag|id>
            admin player set-size <platform> <username|tag|id> <value>
            admin player add-size <platform> <username|tag|id> <value>
            admin player reset-cooldown <platform> <username|tag|id>
            admin player set-last-grow <platform> <username|tag|id> <yyyy-MM-ddTHH:mm:ss|none>
            """.trim();
    }
}
