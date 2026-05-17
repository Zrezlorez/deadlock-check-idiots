package com.litovskiy.repository;

import com.litovskiy.entity.Player;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Player findById(long id);
    List<Player> findAllByIdIn(List<Long> ids);

    Player findByTelegramChatId(long telegramChatId);
    Player findByDiscordUserId(long discordUserId);

    Player findByTelegramUsername(String username);
    Player findByDiscordTag(String tag);

    List<Player> findByTelegramChatIdNotNullOrderBySizeDesc(Limit limit);
    List<Player> findByDiscordUserIdNotNullOrderBySizeDesc(Limit limit);

//    public void mergeAndDeleteSource(Player targetPlayer, Player sourcePlayer) {
//        executeVoid(session -> {
//            Player attachedTarget = session.get(Player.class, targetPlayer.getChatId());
//            if (attachedTarget == null) {
//                throw new IllegalStateException("Target player not found: " + targetPlayer.getChatId());
//            }
//
//            Player attachedSource = session.get(Player.class, sourcePlayer.getChatId());
//            if (attachedSource != null && !attachedSource.getChatId().equals(attachedTarget.getChatId())) {
//                mergeActivityStats(session, attachedSource.getChatId(), attachedTarget.getChatId());
//                mergeConversationParticipants(session, attachedSource.getChatId(), attachedTarget.getChatId());
//                mergeVoiceSessions(session, attachedSource.getChatId(), attachedTarget.getChatId());
//                attachedSource.setTelegramChatId(null);
//                attachedSource.setDiscordUserId(null);
//                session.flush();
//            }
//
//            copyState(targetPlayer, attachedTarget);
//
//            if (attachedSource != null && !attachedSource.getChatId().equals(attachedTarget.getChatId())) {
//                session.remove(attachedSource);
//            }
//        });
//    }
//
//
//    private void copyState(Player source, Player target) {
//        target.setSize(source.getSize());
//        target.setLastGrowTime(source.getLastGrowTime());
//        if (source.getLastAbilityTime() != null
//            && (target.getLastAbilityTime() == null || source.getLastAbilityTime().isAfter(target.getLastAbilityTime()))) {
//            target.setLastAbilityTime(source.getLastAbilityTime());
//        }
//        target.setTelegramChatId(source.getTelegramChatId());
//        target.setDiscordUserId(source.getDiscordUserId());
//        target.setPendingFailChancePenalty(Math.max(target.getPendingFailChancePenalty(), source.getPendingFailChancePenalty()));
//        target.setPendingCritChanceBonus(Math.max(target.getPendingCritChanceBonus(), source.getPendingCritChanceBonus()));
//        target.setPendingGrowthPenalty(Math.max(target.getPendingGrowthPenalty(), source.getPendingGrowthPenalty()));
//        if (source.getTelegramDisplayName() != null) {
//            target.setTelegramDisplayName(source.getTelegramDisplayName());
//        }
//        if (source.getTelegramUsername() != null) {
//            target.setTelegramUsername(source.getTelegramUsername());
//        }
//        if (source.getDiscordTag() != null) {
//            target.setDiscordTag(source.getDiscordTag());
//        }
//    }
//
//    private void mergeActivityStats(org.hibernate.Session session, long sourcePlayerChatId, long targetPlayerChatId) {
//        List<ActivityStat> sourceStats = session.createQuery(
//                "from ActivityStat a where a.playerChatId = :playerChatId",
//                ActivityStat.class)
//            .setParameter("playerChatId", sourcePlayerChatId)
//            .getResultList();
//
//        for (ActivityStat sourceStat : sourceStats) {
//            ActivityStat targetStat = session.createQuery(
//                    "from ActivityStat a where a.playerChatId = :playerChatId and a.platform = :platform "
//                        + "and a.scopeId = :scopeId and a.activityDate = :activityDate",
//                    ActivityStat.class)
//                .setParameter("playerChatId", targetPlayerChatId)
//                .setParameter("platform", sourceStat.getPlatform())
//                .setParameter("scopeId", sourceStat.getScopeId())
//                .setParameter("activityDate", sourceStat.getActivityDate())
//                .setMaxResults(1)
//                .uniqueResult();
//
//            if (targetStat == null) {
//                sourceStat.setPlayerChatId(targetPlayerChatId);
//                continue;
//            }
//
//            targetStat.setMessageCount(targetStat.getMessageCount() + sourceStat.getMessageCount());
//            targetStat.setVoiceSeconds(targetStat.getVoiceSeconds() + sourceStat.getVoiceSeconds());
//            session.remove(sourceStat);
//        }
//    }
//
//    private void mergeVoiceSessions(org.hibernate.Session session, long sourcePlayerChatId, long targetPlayerChatId) {
//        List<VoiceSession> sourceSessions = session.createQuery(
//                "from VoiceSession v where v.playerChatId = :playerChatId",
//                VoiceSession.class)
//            .setParameter("playerChatId", sourcePlayerChatId)
//            .getResultList();
//
//        for (VoiceSession sourceSession : sourceSessions) {
//            VoiceSession targetSession = session.createQuery(
//                    "from VoiceSession v where v.playerChatId = :playerChatId and v.platform = :platform",
//                    VoiceSession.class)
//                .setParameter("playerChatId", targetPlayerChatId)
//                .setParameter("platform", sourceSession.getPlatform())
//                .setMaxResults(1)
//                .uniqueResult();
//
//            if (targetSession == null) {
//                sourceSession.setPlayerChatId(targetPlayerChatId);
//                continue;
//            }
//
//            if (sourceSession.getStartedAt().isBefore(targetSession.getStartedAt())) {
//                targetSession.setStartedAt(sourceSession.getStartedAt());
//            }
//            session.remove(sourceSession);
//        }
//    }
//
//    private void mergeConversationParticipants(org.hibernate.Session session, long sourcePlayerChatId, long targetPlayerChatId) {
//        List<ConversationParticipant> sourceParticipants = session.createQuery(
//                "from ConversationParticipant c where c.playerChatId = :playerChatId",
//                ConversationParticipant.class)
//            .setParameter("playerChatId", sourcePlayerChatId)
//            .getResultList();
//
//        for (ConversationParticipant sourceParticipant : sourceParticipants) {
//            ConversationParticipant targetParticipant = session.createQuery(
//                    "from ConversationParticipant c where c.playerChatId = :playerChatId and c.platform = :platform "
//                        + "and c.scopeId = :scopeId",
//                    ConversationParticipant.class)
//                .setParameter("playerChatId", targetPlayerChatId)
//                .setParameter("platform", sourceParticipant.getPlatform())
//                .setParameter("scopeId", sourceParticipant.getScopeId())
//                .setMaxResults(1)
//                .uniqueResult();
//
//            if (targetParticipant == null) {
//                sourceParticipant.setPlayerChatId(targetPlayerChatId);
//                continue;
//            }
//
//            session.remove(sourceParticipant);
//        }
//    }
//
}
