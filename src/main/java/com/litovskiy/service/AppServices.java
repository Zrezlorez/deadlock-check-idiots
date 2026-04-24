package com.litovskiy.service;

import com.litovskiy.dao.ActivityStatDao;
import com.litovskiy.dao.AppSettingDao;
import com.litovskiy.dao.ConversationSettingsDao;
import com.litovskiy.dao.LinkCodeDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.dao.VoiceSessionDao;
import lombok.Getter;

@Getter
public class AppServices {

    private final PlayerDao playerDao;
    private final GameConfigService gameConfigService;
    private final PlayerAccountService playerAccountService;
    private final ConversationStyleService conversationStyleService;
    private final ActivityService activityService;
    private final DickService dickService;
    private final LinkService linkService;
    private final AdminCommandService adminCommandService;

    public AppServices() {
        this.playerDao = new PlayerDao();
        this.gameConfigService = new GameConfigService(new AppSettingDao());
        this.playerAccountService = new PlayerAccountService(playerDao, gameConfigService);
        this.conversationStyleService = new ConversationStyleService(new ConversationSettingsDao());
        this.activityService = new ActivityService(
            playerAccountService,
            new ActivityStatDao(),
            new VoiceSessionDao(),
            gameConfigService
        );
        this.dickService = new DickService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService
        );
        this.linkService = new LinkService(
            playerDao,
            new LinkCodeDao(),
            playerAccountService,
            gameConfigService
        );
        this.adminCommandService = new AdminCommandService(
            new AdminAccessService(),
            gameConfigService,
            playerDao
        );
    }
}
