package com.litovskiy.service;

import com.litovskiy.dao.ConversationParticipantDao;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;

public class ConversationParticipantService {

    private final PlayerAccountService playerAccountService;
    private final ConversationParticipantDao conversationParticipantDao;

    public ConversationParticipantService(PlayerAccountService playerAccountService,
                                          ConversationParticipantDao conversationParticipantDao) {
        this.playerAccountService = playerAccountService;
        this.conversationParticipantDao = conversationParticipantDao;
    }

    public void registerParticipant(Platform platform, long profileId, long scopeId) {
        Player player = playerAccountService.resolveOrCreate(platform, profileId);
        conversationParticipantDao.save(player.getChatId(), platform, scopeId);
    }
}
