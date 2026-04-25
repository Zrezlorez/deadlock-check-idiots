package com.litovskiy.service;

import com.litovskiy.dao.ActivityStatDao;
import com.litovskiy.dao.AppSettingDao;
import com.litovskiy.dao.ConversationSettingsDao;
import com.litovskiy.dao.LinkCodeDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.dao.VoiceSessionDao;

public class AppServices {

    private final PlayerDao playerDao;
    private final GameConfigService gameConfigService;
    private final PlayerAccountService playerAccountService;
    private final ConversationStyleService conversationStyleService;
    private final ActivityService activityService;
    private final DickService dickService;
    private final LeaderboardService leaderboardService;
    private final LinkService linkService;
    private final AdminCommandService adminCommandService;

    public AppServices() {
        ActivityStatDao activityStatDao = new ActivityStatDao();
        this.playerDao = new PlayerDao();
        this.gameConfigService = new GameConfigService(new AppSettingDao());
        this.playerAccountService = new PlayerAccountService(playerDao, gameConfigService);
        this.conversationStyleService = new ConversationStyleService(new ConversationSettingsDao());
        this.activityService = new ActivityService(
            playerAccountService,
            activityStatDao,
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
        this.leaderboardService = new LeaderboardService(
            playerDao,
            activityStatDao,
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

    public PlayerDao playerDao() {
        return playerDao;
    }

    public GameConfigService gameConfigService() {
        return gameConfigService;
    }

    public PlayerAccountService playerAccountService() {
        return playerAccountService;
    }

    public ConversationStyleService conversationStyleService() {
        return conversationStyleService;
    }

    public ActivityService activityService() {
        return activityService;
    }

    public DickService dickService() {
        return dickService;
    }

    public LeaderboardService leaderboardService() {
        return leaderboardService;
    }

    public LinkService linkService() {
        return linkService;
    }

    public AdminCommandService adminCommandService() {
        return adminCommandService;
    }
}
