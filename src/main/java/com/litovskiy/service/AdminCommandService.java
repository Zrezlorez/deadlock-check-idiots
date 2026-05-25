package com.litovskiy.service;

import com.litovskiy.service.data.PlayerService;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.log.GameLogService;
import com.litovskiy.util.CommandResult;
import com.litovskiy.util.GameSetting;
import com.litovskiy.util.SettingGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCommandService {

    private final AdminAccessService adminAccessService;
    private final GameConfigService gameConfigService;
    private final PlayerService playerDao;
    private final DateTimeFormatter formatter;
    private final GameLogService gameLogService;

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
            case "config" -> reply(availableSettings());
            case "set" -> reply(setConfig(parts));
            case "reset" -> reply(resetConfig(parts));
            case "player" -> reply(handlePlayer(parts));
            case "help" -> reply(help());
            default -> reply("Неизвестная admin-команда.\n" + help());
        };
    }

    private CommandResult reply(String text) {
        return CommandResult.single(text);
    }

    private String setConfig(String[] parts) {
        if (parts.length < 3) {
            return "Использование: admin set <key> <value>";
        }

        GameSetting setting = GameSetting.fromKey(parts[1]);
        if (setting == null) {
            return "Неизвестная настройка. Доступны: \n" + availableSettings();
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
            return "Неизвестная настройка. Доступны: \n" + availableSettings();
        }

        gameConfigService.reset(setting);
        return "Настройка сброшена к дефолту: " + setting.getKey() + " = " + setting.getDefaultValue();
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

    private String availableSettings() {
        return Arrays.stream(GameSetting.values())
            .collect(Collectors.groupingBy(
                GameSetting::getGroup,
                LinkedHashMap::new,
                Collectors.toList()
            ))
            .entrySet()
            .stream()
            .map(entry -> formatGroup(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining("\n\n"));
    }

    private String formatGroup(SettingGroup group, List<GameSetting> settings) {
        StringBuilder builder = new StringBuilder();

        builder.append(group.getDisplayName()).append("\n");

        for (GameSetting setting : settings) {
            builder.append("• ")
                .append(setting.getKey())
                .append(" = ")
                .append(gameConfigService.getRawValue(setting))
                .append("\n  ")
                .append(setting.getDescription())
                .append("\n");
        }

        return builder.toString().trim();
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
