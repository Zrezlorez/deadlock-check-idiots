package com.litovskiy.dao;

import com.litovskiy.entity.Platform;
import com.litovskiy.entity.VoiceSession;

import java.util.ArrayList;
import java.util.List;

public class VoiceSessionDao extends BaseDao {

    public VoiceSession findActiveSession(long playerChatId, Platform platform) {
        return execute(session -> session.createQuery(
                "from VoiceSession v where v.playerChatId = :playerChatId and v.platform = :platform",
                VoiceSession.class)
            .setParameter("playerChatId", playerChatId)
            .setParameter("platform", platform)
            .setMaxResults(1)
            .uniqueResult());
    }

    public List<VoiceSession> findActiveSessions(Platform platform, long scopeId) {
        return execute(session -> session.createQuery(
                "from VoiceSession v where v.platform = :platform and v.scopeId = :scopeId",
                VoiceSession.class)
            .setParameter("platform", platform)
            .setParameter("scopeId", scopeId)
            .getResultList());
    }

    public List<Long> findActiveScopeIdsByPlayer(long playerChatId, Platform platform) {
        return execute(session -> new ArrayList<>(session.createQuery(
                "select distinct v.scopeId from VoiceSession v where v.playerChatId = :playerChatId and v.platform = :platform",
                Long.class)
            .setParameter("playerChatId", playerChatId)
            .setParameter("platform", platform)
            .getResultList()));
    }

    public void save(VoiceSession voiceSession) {
        executeVoid(session -> session.merge(voiceSession));
    }

    public void delete(VoiceSession voiceSession) {
        executeVoid(session -> {
            VoiceSession attached = session.contains(voiceSession) ? voiceSession : session.merge(voiceSession);
            session.remove(attached);
        });
    }
}
