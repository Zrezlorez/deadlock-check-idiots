package com.litovskiy.repository;

import com.litovskiy.entity.TelegramCallbackVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TelegramCallbackVoteRepository extends JpaRepository<TelegramCallbackVote, Long> {
    TelegramCallbackVote findByRequestIdAndPlayerId(long requestId, long playerId);
    TelegramCallbackVote findByRequestIdAndIsMotherTrue(long requestId);
    List<TelegramCallbackVote> findByRequestId(long requestId);
}
