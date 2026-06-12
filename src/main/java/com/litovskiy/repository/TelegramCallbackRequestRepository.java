package com.litovskiy.repository;

import com.litovskiy.entity.TelegramCallbackRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramCallbackRequestRepository extends JpaRepository<TelegramCallbackRequest, Long> {
    TelegramCallbackRequest findByScopeIdAndMessageId(long scopeId, long messageId);
}
