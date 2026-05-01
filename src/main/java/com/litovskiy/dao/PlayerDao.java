package com.litovskiy.dao;

import com.litovskiy.entity.ActivityStat;
import com.litovskiy.entity.ConversationParticipant;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.VoiceSession;

import java.util.ArrayList;
import java.util.List;

public class PlayerDao extends BaseDao {

    public Player find(long id) {
        return execute(session -> session.get(Player.class, id));
    }

    public Player findByPlatform(Platform platform, long profileId) {
        return switch (platform) {
            case TELEGRAM -> findByField("telegramChatId", profileId);
            case DISCORD -> findByField("discordUserId", profileId);
        };
    }

    public Player findByTelegramUsername(String username) {
        return findByNormalizedField("telegramUsername", normalizeHandle(username));
    }

    public Player findByDiscordTag(String tag) {
        return findByNormalizedField("discordTag", normalizeTag(tag));
    }

    public Player findLegacy(long profileId) {
        return execute(session -> session.createQuery(
                "from Player p where p.chatId = :profileId and p.telegramChatId is null and p.discordUserId is null",
                Player.class)
            .setParameter("profileId", profileId)
            .uniqueResult());
    }

    public void save(Player player) {
        executeVoid(session -> session.merge(player));
    }

    public List<Player> findByChatIds(List<Long> chatIds) {
        if (chatIds == null || chatIds.isEmpty()) {
            return List.of();
        }

        return execute(session -> new ArrayList<>(session.createQuery(
                "from Player p where p.chatId in :chatIds",
                Player.class)
            .setParameter("chatIds", chatIds)
            .getResultList()));
    }

    public List<Player> findTopByPlatform(Platform platform, int limit) {
        String fieldName = switch (platform) {
            case TELEGRAM -> "telegramChatId";
            case DISCORD -> "discordUserId";
        };

        return execute(session -> new ArrayList<>(session.createQuery(
                "from Player p where p." + fieldName + " is not null order by p.size desc",
                Player.class)
            .setMaxResults(limit)
            .getResultList()));
    }

    public void mergeAndDeleteSource(Player targetPlayer, Player sourcePlayer) {
        executeVoid(session -> {
            Player attachedTarget = session.get(Player.class, targetPlayer.getChatId());
            if (attachedTarget == null) {
                throw new IllegalStateException("Target player not found: " + targetPlayer.getChatId());
            }

            Player attachedSource = session.get(Player.class, sourcePlayer.getChatId());
            if (attachedSource != null && !attachedSource.getChatId().equals(attachedTarget.getChatId())) {
                mergeActivityStats(session, attachedSource.getChatId(), attachedTarget.getChatId());
                mergeConversationParticipants(session, attachedSource.getChatId(), attachedTarget.getChatId());
                mergeVoiceSessions(session, attachedSource.getChatId(), attachedTarget.getChatId());
                attachedSource.setTelegramChatId(null);
                attachedSource.setDiscordUserId(null);
                session.flush();
            }

            copyState(targetPlayer, attachedTarget);

            if (attachedSource != null && !attachedSource.getChatId().equals(attachedTarget.getChatId())) {
                session.remove(attachedSource);
            }
        });
    }

    private Player findByField(String fieldName, Object value) {
        return execute(session -> session.createQuery(
                "from Player p where p." + fieldName + " = :value",
                Player.class)
            .setParameter("value", value)
            .setMaxResults(1)
            .uniqueResult());
    }

    private Player findByNormalizedField(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return execute(session -> session.createQuery(
                "from Player p where lower(p." + fieldName + ") = :value",
                Player.class)
            .setParameter("value", value.toLowerCase())
            .setMaxResults(1)
            .uniqueResult());
    }

    private void copyState(Player source, Player target) {
        target.setSize(source.getSize());
        target.setLastGrowTime(source.getLastGrowTime());
        if (source.getLastAbilityTime() != null
            && (target.getLastAbilityTime() == null || source.getLastAbilityTime().isAfter(target.getLastAbilityTime()))) {
            target.setLastAbilityTime(source.getLastAbilityTime());
        }
        target.setTelegramChatId(source.getTelegramChatId());
        target.setDiscordUserId(source.getDiscordUserId());
        target.setPendingFailChanceBonus(Math.max(target.getPendingFailChanceBonus(), source.getPendingFailChanceBonus()));
        target.setPendingCritChanceBonus(Math.max(target.getPendingCritChanceBonus(), source.getPendingCritChanceBonus()));
        target.setPendingGrowthPenalty(Math.max(target.getPendingGrowthPenalty(), source.getPendingGrowthPenalty()));
        if (source.getTelegramDisplayName() != null) {
            target.setTelegramDisplayName(source.getTelegramDisplayName());
        }
        if (source.getTelegramUsername() != null) {
            target.setTelegramUsername(source.getTelegramUsername());
        }
        if (source.getDiscordTag() != null) {
            target.setDiscordTag(source.getDiscordTag());
        }
    }

    private void mergeActivityStats(org.hibernate.Session session, long sourcePlayerChatId, long targetPlayerChatId) {
        List<ActivityStat> sourceStats = session.createQuery(
                "from ActivityStat a where a.playerChatId = :playerChatId",
                ActivityStat.class)
            .setParameter("playerChatId", sourcePlayerChatId)
            .getResultList();

        for (ActivityStat sourceStat : sourceStats) {
            ActivityStat targetStat = session.createQuery(
                    "from ActivityStat a where a.playerChatId = :playerChatId and a.platform = :platform "
                        + "and a.scopeId = :scopeId and a.activityDate = :activityDate",
                    ActivityStat.class)
                .setParameter("playerChatId", targetPlayerChatId)
                .setParameter("platform", sourceStat.getPlatform())
                .setParameter("scopeId", sourceStat.getScopeId())
                .setParameter("activityDate", sourceStat.getActivityDate())
                .setMaxResults(1)
                .uniqueResult();

            if (targetStat == null) {
                sourceStat.setPlayerChatId(targetPlayerChatId);
                continue;
            }

            targetStat.setMessageCount(targetStat.getMessageCount() + sourceStat.getMessageCount());
            targetStat.setVoiceSeconds(targetStat.getVoiceSeconds() + sourceStat.getVoiceSeconds());
            session.remove(sourceStat);
        }
    }

    private void mergeVoiceSessions(org.hibernate.Session session, long sourcePlayerChatId, long targetPlayerChatId) {
        List<VoiceSession> sourceSessions = session.createQuery(
                "from VoiceSession v where v.playerChatId = :playerChatId",
                VoiceSession.class)
            .setParameter("playerChatId", sourcePlayerChatId)
            .getResultList();

        for (VoiceSession sourceSession : sourceSessions) {
            VoiceSession targetSession = session.createQuery(
                    "from VoiceSession v where v.playerChatId = :playerChatId and v.platform = :platform",
                    VoiceSession.class)
                .setParameter("playerChatId", targetPlayerChatId)
                .setParameter("platform", sourceSession.getPlatform())
                .setMaxResults(1)
                .uniqueResult();

            if (targetSession == null) {
                sourceSession.setPlayerChatId(targetPlayerChatId);
                continue;
            }

            if (sourceSession.getStartedAt().isBefore(targetSession.getStartedAt())) {
                targetSession.setStartedAt(sourceSession.getStartedAt());
            }
            session.remove(sourceSession);
        }
    }

    private void mergeConversationParticipants(org.hibernate.Session session, long sourcePlayerChatId, long targetPlayerChatId) {
        List<ConversationParticipant> sourceParticipants = session.createQuery(
                "from ConversationParticipant c where c.playerChatId = :playerChatId",
                ConversationParticipant.class)
            .setParameter("playerChatId", sourcePlayerChatId)
            .getResultList();

        for (ConversationParticipant sourceParticipant : sourceParticipants) {
            ConversationParticipant targetParticipant = session.createQuery(
                    "from ConversationParticipant c where c.playerChatId = :playerChatId and c.platform = :platform "
                        + "and c.scopeId = :scopeId",
                    ConversationParticipant.class)
                .setParameter("playerChatId", targetPlayerChatId)
                .setParameter("platform", sourceParticipant.getPlatform())
                .setParameter("scopeId", sourceParticipant.getScopeId())
                .setMaxResults(1)
                .uniqueResult();

            if (targetParticipant == null) {
                sourceParticipant.setPlayerChatId(targetPlayerChatId);
                continue;
            }

            session.remove(sourceParticipant);
        }
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
