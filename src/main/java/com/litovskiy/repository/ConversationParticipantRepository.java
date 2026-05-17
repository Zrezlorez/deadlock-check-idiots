package com.litovskiy.repository;

import com.litovskiy.entity.ConversationParticipant;
import com.litovskiy.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    @Query("""
    select cp.playerId
    from ConversationParticipant cp
    where cp.platform = :platform
      and cp.scopeId = :scopeId
""")
    List<Long> findByPlatformAndScopeId(Platform platform, long scopeId);

    boolean existsByPlayerIdAndPlatformAndScopeId(
        long playerId,
        Platform platform,
        long scopeId
    );
}
