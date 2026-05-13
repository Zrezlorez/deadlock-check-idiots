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
        return PropsManager.getProps().getProperty(key);
    }
}
