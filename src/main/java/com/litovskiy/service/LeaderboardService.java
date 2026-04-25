package com.litovskiy.service;

import com.litovskiy.dao.ActivityStatDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class LeaderboardService {

    private final PlayerDao playerDao;
    private final ActivityStatDao activityStatDao;
    private final ConversationStyleService conversationStyleService;
    private final GameConfigService gameConfigService;

    public LeaderboardService(PlayerDao playerDao,
                              ActivityStatDao activityStatDao,
                              ConversationStyleService conversationStyleService,
                              GameConfigService gameConfigService) {
        this.playerDao = playerDao;
        this.activityStatDao = activityStatDao;
        this.conversationStyleService = conversationStyleService;
        this.gameConfigService = gameConfigService;
    }

    public String buildLeaderboard(Platform platform, long requesterProfileId, Long scopeId) {
        int limit = gameConfigService.getInt(GameSetting.LEADERBOARD_LIMIT);
        List<Player> rankedPlayers = scopeId == null
            ? playerDao.findTopByPlatform(platform, limit)
            : findScopeLeaderboard(platform, scopeId);

        if (rankedPlayers.isEmpty()) {
            return scopeId == null
                ? "Глобальный лидерборд пока пуст."
                : "В этой беседе пока нет участников для лидерборда.";
        }

        GrowthStyle style = conversationStyleService.getStyle(platform, scopeId);
        StringBuilder builder = new StringBuilder(scopeId == null
            ? "Глобальный топ по показателю " + style.displayName() + ":\n"
            : "Топ этой беседы по показателю " + style.displayName() + ":\n");

        int rows = Math.min(limit, rankedPlayers.size());
        for (int index = 0; index < rows; index++) {
            Player player = rankedPlayers.get(index);
            builder.append(index + 1)
                .append(". ")
                .append(formatPlayer(platform, player))
                .append(" — ")
                .append(formatValue(player.getSize()));

            if (Objects.equals(platform.getProfileId(player), requesterProfileId)) {
                builder.append(" ← вы");
            }

            builder.append('\n');
        }

        appendRequesterPlace(builder, rankedPlayers, platform, requesterProfileId, rows);
        return builder.toString().trim();
    }

    private List<Player> findScopeLeaderboard(Platform platform, long scopeId) {
        List<Long> participantIds = activityStatDao.findParticipantIds(platform, scopeId);
        return playerDao.findByChatIds(participantIds).stream()
            .filter(player -> platform.getProfileId(player) != null)
            .sorted(Comparator.comparingDouble(Player::getSize).reversed())
            .toList();
    }

    private void appendRequesterPlace(StringBuilder builder,
                                      List<Player> rankedPlayers,
                                      Platform platform,
                                      long requesterProfileId,
                                      int visibleRows) {
        for (int index = 0; index < rankedPlayers.size(); index++) {
            if (Objects.equals(platform.getProfileId(rankedPlayers.get(index)), requesterProfileId) && index >= visibleRows) {
                builder.append("\nВаше место: ")
                    .append(index + 1)
                    .append(" из ")
                    .append(rankedPlayers.size());
                return;
            }
        }
    }

    private String formatPlayer(Platform platform, Player player) {
        Long profileId = platform.getProfileId(player);
        if (profileId == null) {
            return "аккаунт " + player.getChatId();
        }

        return switch (platform) {
            case TELEGRAM -> "id " + profileId;
            case DISCORD -> "<@" + profileId + ">";
        };
    }

    private String formatValue(double value) {
        if (value > 100_000_000) {
            return round(value / 100_000_000) + " к км";
        }

        if (value > 100_000) {
            return round(value / 100_000) + " км";
        }

        if (value > 100) {
            return round(value / 100) + " м";
        }

        return round(value) + " см";
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
