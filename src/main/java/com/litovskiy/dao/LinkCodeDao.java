package com.litovskiy.dao;

import com.litovskiy.entity.LinkCode;

import java.time.LocalDateTime;

public class LinkCodeDao extends BaseDao {

    public LinkCode findByCode(String code) {
        return execute(session -> session.createQuery(
                "from LinkCode lc where lc.code = :code",
                LinkCode.class)
            .setParameter("code", code)
            .setMaxResults(1)
            .uniqueResult());
    }

    public LinkCode findByPlayerChatId(long playerChatId) {
        return execute(session -> session.createQuery(
                "from LinkCode lc where lc.playerChatId = :playerChatId",
                LinkCode.class)
            .setParameter("playerChatId", playerChatId)
            .setMaxResults(1)
            .uniqueResult());
    }

    public void save(LinkCode linkCode) {
        executeVoid(session -> session.merge(linkCode));
    }

    public void delete(LinkCode linkCode) {
        executeVoid(session -> {
            LinkCode attached = session.contains(linkCode) ? linkCode : session.merge(linkCode);
            session.remove(attached);
        });
    }

    public void deleteExpired(LocalDateTime now) {
        executeVoid(session -> session.createMutationQuery(
                "delete from LinkCode lc where lc.expiresAt <= :now")
            .setParameter("now", now)
            .executeUpdate());
    }
}
