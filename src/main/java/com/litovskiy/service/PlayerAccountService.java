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

    public void updateTelegramDisplayName(long profileId, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }

        Player player = resolveOrCreate(Platform.TELEGRAM, profileId);
        if (Objects.equals(player.getTelegramDisplayName(), displayName)) {
            return;
        }

        player.setTelegramDisplayName(displayName);
        playerDao.save(player);
    }
}
