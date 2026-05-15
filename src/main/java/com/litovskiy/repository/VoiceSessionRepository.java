package com.litovskiy.repository;

import com.litovskiy.entity.Platform;
import com.litovskiy.entity.VoiceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface VoiceSessionRepository extends JpaRepository<VoiceSession, Long> {

    Optional<VoiceSession> findByPlatformAndPlayerId(Platform platform, long playerId);

    List<VoiceSession> findByPlatformAndScopeId(Platform platform, long scopeId);

    @Query("""
        select distinct v.scopeId
        from VoiceSession v
        where v.playerId = :playerId
          and v.platform = :platform
    """)
    List<Long> findActiveScopeIdsByPlayer(
        @Param("playerId") long playerId,
        @Param("platform") Platform platform
    );

}
