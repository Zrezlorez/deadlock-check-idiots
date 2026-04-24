package com.litovskiy.dao;

import com.litovskiy.entity.ConversationSettings;
import com.litovskiy.entity.Platform;

public class ConversationSettingsDao extends BaseDao {

    public ConversationSettings findByScope(Platform platform, long scopeId) {
        return execute(session -> session.createQuery(
                "from ConversationSettings c where c.platform = :platform and c.scopeId = :scopeId",
                ConversationSettings.class)
            .setParameter("platform", platform)
            .setParameter("scopeId", scopeId)
            .setMaxResults(1)
            .uniqueResult());
    }

    public void save(ConversationSettings settings) {
        executeVoid(session -> session.merge(settings));
    }
}
