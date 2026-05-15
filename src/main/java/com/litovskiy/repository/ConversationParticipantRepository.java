package com.litovskiy.repository;

import com.litovskiy.entity.ConversationParticipant;
import com.litovskiy.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<Long> findByPlatformAndScopeId(Platform platform, long scopeId);

    boolean existsByPlayerIdAndPlatformAndScopeId(
        long playerId,
        Platform platform,
        long scopeId
    );
}
