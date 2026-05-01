package com.litovskiy.dao;

import com.litovskiy.entity.ConversationParticipant;
import com.litovskiy.entity.Platform;

import java.util.ArrayList;
import java.util.List;

public class ConversationParticipantDao extends BaseDao {

    public void save(long playerChatId, Platform platform, long scopeId) {
        executeVoid(session -> {
            ConversationParticipant participant = session.createQuery(
                    "from ConversationParticipant c where c.playerChatId = :playerChatId and c.platform = :platform "
                        + "and c.scopeId = :scopeId",
                    ConversationParticipant.class)
                .setParameter("playerChatId", playerChatId)
                .setParameter("platform", platform)
                .setParameter("scopeId", scopeId)
                .setMaxResults(1)
                .uniqueResult();

            if (participant == null) {
                session.persist(new ConversationParticipant(playerChatId, platform, scopeId));
            }
        });
    }

    public List<Long> findParticipantIds(Platform platform, long scopeId) {
        return execute(session -> new ArrayList<>(session.createQuery(
                "select c.playerChatId from ConversationParticipant c where c.platform = :platform and c.scopeId = :scopeId",
                Long.class)
            .setParameter("platform", platform)
            .setParameter("scopeId", scopeId)
            .getResultList()));
    }
}
