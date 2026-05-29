package com.litovskiy.service.data;

import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    public void save(Player player) {
        playerRepository.save(player);
    }

    public void delete(Player player) {
        playerRepository.delete(player);
    }

    public Player findById(long id) {
        return playerRepository.findById(id);
    }

    public List<Player> findByIds(List<Long> ids) {
        return playerRepository.findAllByIdIn(ids);
    }

    public Player findByPlatform(Platform platform, long profileId) {
        return switch (platform) {
            case TELEGRAM -> playerRepository.findByTelegramChatId(profileId);
            case DISCORD -> playerRepository.findByDiscordUserId(profileId);
        };
    }

    public Player findByTelegramUsername(String username) {
        return playerRepository.findByTelegramUsername(normalizeHandle(username));
    }

    public Player findByDiscordTag(String username) {
        return playerRepository.findByDiscordTag(normalizeTag(username));
    }

    public List<Player> findTopByPlatform(Platform platform, int limit) {
        return switch (platform) {
            case TELEGRAM -> playerRepository.findByTelegramChatIdNotNullOrderBySizeDesc(Limit.of(limit));
            case DISCORD -> playerRepository.findByDiscordUserIdNotNullOrderBySizeDesc(Limit.of(limit));
        };
    }

    private String normalizeHandle(String username) {
        if (username == null) {
            return null;
        }

        String trimmed = username.trim();
        if (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    private String normalizeTag(String tag) {
        return tag == null ? null : tag.trim();
    }
}
