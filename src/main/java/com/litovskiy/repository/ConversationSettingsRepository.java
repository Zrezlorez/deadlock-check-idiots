package com.litovskiy.repository;

import com.litovskiy.entity.ConversationSettings;
import com.litovskiy.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationSettingsRepository extends JpaRepository<ConversationSettings, Long> {

    ConversationSettings findByPlatformAndScopeId(Platform platform, long scopeId);
}
