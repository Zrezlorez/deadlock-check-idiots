package com.litovskiy.service;

import com.litovskiy.entity.ConversationParticipant;
import com.litovskiy.repository.ConversationParticipantRepository;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationParticipantService {

    private final PlayerAccountService playerAccountService;
    private final ConversationParticipantRepository repository;

    public void registerParticipant(Platform platform, long profileId, long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        if (repository.existsByPlayerIdAndPlatformAndScopeId(player.getId(), platform, scopeId)) {
            return;
        }

        repository.save(new ConversationParticipant(
            player.getId(),
            platform,
            scopeId
        ));
    }
}
