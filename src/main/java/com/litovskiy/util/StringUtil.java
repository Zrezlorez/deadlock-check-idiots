package com.litovskiy.util;

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

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
        StringBuilder builder = new StringBuilder();
        if (!user.getFirstName().isBlank()) {
            builder.append(user.getFirstName().trim());
        }
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(user.getLastName().trim());
        }
        if (!builder.isEmpty()) {
            return builder.toString();
        }
        if (user.getUserName() != null && !user.getUserName().isBlank()) {
            return "@" + user.getUserName().trim();
        }
        return "Пользователь " + user.getId();
    }

    public static String formatDiscordTag(net.dv8tion.jda.api.entities.User user) {
        String discriminator = user.getDiscriminator();
        if (discriminator.equals("0")) {
            return user.getName();
        }
        return user.getName() + "#" + discriminator;
    }
}
