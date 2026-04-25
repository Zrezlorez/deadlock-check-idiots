package com.litovskiy.service;

import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;

import java.util.Objects;

public class PlayerAccountService {

    private final PlayerDao playerDao;
    private final GameConfigService gameConfigService;

    public PlayerAccountService(PlayerDao playerDao, GameConfigService gameConfigService) {
        this.playerDao = playerDao;
        this.gameConfigService = gameConfigService;
    }

    public Player resolveOrCreate(Platform platform, long profileId) {
        Player player = playerDao.findByPlatform(platform, profileId);
        if (player != null) {
            return player;
        }

        Player legacyPlayer = playerDao.findLegacy(profileId);
        if (legacyPlayer != null) {
            platform.setProfileId(legacyPlayer, profileId);
            playerDao.save(legacyPlayer);
            return legacyPlayer;
        }

        Player newPlayer = new Player(profileId, gameConfigService.getDouble(GameSetting.START_SIZE));
        platform.setProfileId(newPlayer, profileId);
        playerDao.save(newPlayer);
        return newPlayer;
    }

    public void updateTelegramProfile(long profileId, String displayName, String username) {
        Player player = resolveOrCreate(Platform.TELEGRAM, profileId);
        boolean changed = false;

        if (displayName != null && !displayName.isBlank() && !Objects.equals(player.getTelegramDisplayName(), displayName)) {
            player.setTelegramDisplayName(displayName);
            changed = true;
        }

        String normalizedUsername = normalizeTelegramUsername(username);
        if (!Objects.equals(player.getTelegramUsername(), normalizedUsername)) {
            player.setTelegramUsername(normalizedUsername);
            changed = true;
        }

        if (changed) {
            playerDao.save(player);
        }
    }

    public void updateDiscordTag(long profileId, String discordTag) {
        String normalizedDiscordTag = normalizeDiscordTag(discordTag);
        if (normalizedDiscordTag == null) {
            return;
        }

        Player player = resolveOrCreate(Platform.DISCORD, profileId);
        if (Objects.equals(player.getDiscordTag(), normalizedDiscordTag)) {
            return;
        }

        player.setDiscordTag(normalizedDiscordTag);
        playerDao.save(player);
    }

    private String normalizeTelegramUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String normalized = username.trim();
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }

    private String normalizeDiscordTag(String discordTag) {
        if (discordTag == null || discordTag.isBlank()) {
            return null;
        }

        return discordTag.trim();
    }
}
