package com.litovskiy.dao;

import com.litovskiy.entity.ActivityStat;
import com.litovskiy.entity.Platform;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityStatDao extends BaseDao {

    public void incrementMessages(long playerChatId, Platform platform, long scopeId, LocalDate activityDate, long amount) {
        updateTotals(playerChatId, platform, scopeId, activityDate, amount, 0);
    }

    public void incrementVoiceSeconds(long playerChatId, Platform platform, long scopeId, LocalDate activityDate, long seconds) {
        updateTotals(playerChatId, platform, scopeId, activityDate, 0, seconds);
    }

    public Map<Long, Long> findMessageTotals(Platform platform, long scopeId, LocalDate fromDate) {
        return findTotals(
            "select a.playerChatId, sum(a.messageCount) from ActivityStat a "
                + "where a.platform = :platform and a.scopeId = :scopeId and a.activityDate >= :fromDate "
                + "group by a.playerChatId",
            platform,
            scopeId,
            fromDate
        );
    }

    public Map<Long, Long> findVoiceTotals(Platform platform, long scopeId, LocalDate fromDate) {
        return findTotals(
            "select a.playerChatId, sum(a.voiceSeconds) from ActivityStat a "
                + "where a.platform = :platform and a.scopeId = :scopeId and a.activityDate >= :fromDate "
                + "group by a.playerChatId",
            platform,
            scopeId,
            fromDate
        );
    }

    public List<Long> findScopeIdsByPlayer(long playerChatId, Platform platform, LocalDate fromDate) {
        return execute(session -> new ArrayList<>(session.createQuery(
                "select distinct a.scopeId from ActivityStat a where a.playerChatId = :playerChatId "
                    + "and a.platform = :platform and a.activityDate >= :fromDate",
                Long.class)
            .setParameter("playerChatId", playerChatId)
            .setParameter("platform", platform)
            .setParameter("fromDate", fromDate)
            .getResultList()));
    }

    private void updateTotals(long playerChatId,
                              Platform platform,
                              long scopeId,
                              LocalDate activityDate,
                              long messageDelta,
                              long voiceDelta) {
        executeVoid(session -> {
            ActivityStat stat = session.createQuery(
                    "from ActivityStat a where a.playerChatId = :playerChatId and a.platform = :platform "
                        + "and a.scopeId = :scopeId and a.activityDate = :activityDate",
                    ActivityStat.class)
                .setParameter("playerChatId", playerChatId)
                .setParameter("platform", platform)
                .setParameter("scopeId", scopeId)
                .setParameter("activityDate", activityDate)
                .setMaxResults(1)
                .uniqueResult();

            if (stat == null) {
                stat = new ActivityStat(playerChatId, platform, scopeId, activityDate);
            }

            stat.setMessageCount(stat.getMessageCount() + messageDelta);
            stat.setVoiceSeconds(stat.getVoiceSeconds() + voiceDelta);
            session.merge(stat);
        });
    }

    private Map<Long, Long> findTotals(String query, Platform platform, long scopeId, LocalDate fromDate) {
        return execute(session -> {
            List<Object[]> rows = session.createQuery(query, Object[].class)
                .setParameter("platform", platform)
                .setParameter("scopeId", scopeId)
                .setParameter("fromDate", fromDate)
                .getResultList();

            Map<Long, Long> result = new HashMap<>();
            for (Object[] row : rows) {
                result.put((Long) row[0], ((Number) row[1]).longValue());
            }
            return result;
        });
    }
}
