package com.litovskiy.util;

public final class BotConfig {

    private BotConfig() {
    }

    public static String telegramToken() {
        return require("telegram.token");
    }

    public static String discordToken() {
        return require("discord.token");
    }

    public static boolean isProxyEnabled() {
        return Boolean.parseBoolean(PropsManager.getProps().getProperty("proxy.isEnabled", "false"));
    }

    private static String require(String key) {
        String value = PropsManager.getProps().getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }
}
