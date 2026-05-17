package com.litovskiy.repository;

import com.litovskiy.entity.ActivityStat;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.PlayerTotalProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityStatRepository extends JpaRepository<ActivityStat, Long> {

    List<ActivityStat> findByPlayerId(long playerId);

    void deleteByPlayerId(long playerId);

    Optional<ActivityStat> findByPlayerIdAndPlatformAndScopeIdAndActivityDate(
        long id,
        Platform platform,
        long scopeId,
        LocalDate activityDate
    );

    @Query("""
        select a.playerId as playerId, coalesce(sum(a.messageCount), 0) as total
        from ActivityStat a
        where a.platform = :platform
          and a.scopeId = :scopeId
          and a.activityDate >= :fromDate
        group by a.playerId
    """)
    List<PlayerTotalProjection> findMessageTotalRows(
        @Param("platform") Platform platform,
        @Param("scopeId") long scopeId,
        @Param("fromDate") LocalDate fromDate
    );

    @Query("""
        select a.playerId as playerId, coalesce(sum(a.voiceSeconds), 0) as total
        from ActivityStat a
        where a.platform = :platform
          and a.scopeId = :scopeId
          and a.activityDate >= :fromDate
        group by a.playerId
    """)
    List<PlayerTotalProjection> findVoiceTotalRows(
        @Param("platform") Platform platform,
        @Param("scopeId") long scopeId,
        @Param("fromDate") LocalDate fromDate
    );

    @Query("""
        select distinct a.scopeId
        from ActivityStat a
        where a.id = :playerChatId
          and a.platform = :platform
          and a.activityDate >= :fromDate
    """)
    List<Long> findScopeIdsByPlayer(
        @Param("playerChatId") long id,
        @Param("platform") Platform platform,
        @Param("fromDate") LocalDate fromDate
    );

    @Query("""
        select distinct a.id
        from ActivityStat a
        where a.platform = :platform
          and a.scopeId = :scopeId
    """)
    List<Long> findParticipantIds(
        @Param("platform") Platform platform,
        @Param("scopeId") long scopeId
    );

    @Modifying
    @Query(value = """
    INSERT INTO activity_stats (
        player_id,
        platform,
        scope_id,
        activity_date,
        message_count,
        voice_seconds
    )
    VALUES (
        :playerId,
        :platform,
        :scopeId,
        :activityDate,
        :amount,
        0
    )
    ON CONFLICT (player_id, platform, scope_id, activity_date)
    DO UPDATE SET
        message_count = activity_stats.message_count + EXCLUDED.message_count
    """, nativeQuery = true)
    void incrementMessages(
        @Param("playerId") long playerId,
        @Param("platform") Platform platform,
        @Param("scopeId") long scopeId,
        @Param("activityDate") LocalDate activityDate,
        @Param("amount") long amount
    );
}
