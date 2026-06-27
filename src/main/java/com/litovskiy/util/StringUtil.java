package com.litovskiy.util;

import com.litovskiy.entity.Player;
import org.telegram.telegrambots.meta.api.objects.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

public final class StringUtil {

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    public static double round(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    public static String formatDuration(Duration duration) {
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

    public static String formatTelegramDisplayName(User user) {
        return formatTelegramDisplayName(
            user.getFirstName(),
            user.getLastName(),
            user.getUserName(),
            user.getId()
        );
    }

    public static String formatTelegramDisplayName(String firstName, String lastName, String username, long userId) {
        StringBuilder builder = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            builder.append(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(lastName.trim());
        }
        if (!builder.isEmpty()) {
            return builder.toString();
        }
        if (username != null && !username.isBlank()) {
            return "@" + username.trim();
        }
        return "Пользователь " + userId;
    }

    public static String formatTelegramPlayer(Player player, Long scopeId) {
        String displayName = player.getTelegramDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = "id " + player.getTelegramChatId();
        }

        if (scopeId != null) {
            return displayName;
        }

        return "<a href=\"tg://user?id=" + player.getTelegramChatId() + "\">" + escapeHtml(displayName) + "</a>";
    }

    public static String formatDiscordTag(net.dv8tion.jda.api.entities.User user) {
        String discriminator = user.getDiscriminator();
        if (discriminator.equals("0")) {
            return user.getName();
        }
        return user.getName() + "#" + discriminator;
    }
}
