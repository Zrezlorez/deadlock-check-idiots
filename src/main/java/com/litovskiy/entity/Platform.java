package com.litovskiy.entity;

public enum Platform {
    TELEGRAM,
    DISCORD;

    public Long getProfileId(Player player) {
        return switch (this) {
            case TELEGRAM -> player.getTelegramChatId();
            case DISCORD -> player.getDiscordUserId();
        };
    }

    public void setProfileId(Player player, Long profileId) {
        switch (this) {
            case TELEGRAM -> player.setTelegramChatId(profileId);
            case DISCORD -> player.setDiscordUserId(profileId);
        }
    }

    public String displayName() {
        return switch (this) {
            case TELEGRAM -> "Telegram";
            case DISCORD -> "Discord";
        };
    }
}
