package com.litovskiy.service;

import com.litovskiy.config.properties.GrowthProperties;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.data.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerAccountService {

    private final PlayerService playerService;
    private final GrowthProperties growthProperties;

    public Player resolveOrCreate(Platform platform, long profileId) {
        Player player = playerService.findByPlatform(platform, profileId);
        if (player != null) {
            return player;
        }

        Player newPlayer = new Player(growthProperties.getStartSize());
        if (platform == Platform.TELEGRAM) {
            newPlayer.setTelegramChatId(profileId);
        } else if (platform == Platform.DISCORD) {
            newPlayer.setDiscordUserId(profileId);
        }
        playerService.save(newPlayer);
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
            playerService.save(player);
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
        playerService.save(player);
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
