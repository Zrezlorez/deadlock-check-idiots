package com.litovskiy.service;

import com.litovskiy.dao.ActivityStatDao;
import com.litovskiy.dao.AppSettingDao;
import com.litovskiy.dao.ConversationParticipantDao;
import com.litovskiy.dao.ConversationSettingsDao;
import com.litovskiy.dao.LinkCodeDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.dao.VoiceSessionDao;

public class AppServices {

    private final PlayerDao playerDao;
    private final GameConfigService gameConfigService;
    private final PlayerAccountService playerAccountService;
    private final ConversationStyleService conversationStyleService;
    private final ConversationParticipantService conversationParticipantService;
    private final ActivityService activityService;
    private final AbilityService abilityService;
    private final GrowService growService;
    private final LeaderboardService leaderboardService;
    private final LinkService linkService;
    private final AdminCommandService adminCommandService;

    public AppServices() {
        ActivityStatDao activityStatDao = new ActivityStatDao();
        ConversationParticipantDao conversationParticipantDao = new ConversationParticipantDao();
        this.playerDao = new PlayerDao();
        this.gameConfigService = new GameConfigService(new AppSettingDao());
        this.playerAccountService = new PlayerAccountService(playerDao, gameConfigService);
        this.conversationStyleService = new ConversationStyleService(new ConversationSettingsDao());
        this.conversationParticipantService = new ConversationParticipantService(
            playerAccountService,
            conversationParticipantDao
        );
        this.activityService = new ActivityService(
            playerAccountService,
            activityStatDao,
            new VoiceSessionDao(),
            gameConfigService
        );
        this.abilityService = new AbilityService(
            playerDao,
            playerAccountService,
            conversationParticipantDao,
            gameConfigService,
            conversationStyleService
        );
        this.growService = new GrowService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService
        );
        this.leaderboardService = new LeaderboardService(
            playerDao,
            conversationParticipantDao,
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

    public ConversationParticipantService conversationParticipantService() {
        return conversationParticipantService;
    }

    public ActivityService activityService() {
        return activityService;
    }

    public AbilityService abilityService() {
        return abilityService;
    }

    public GrowService dickService() {
        return growService;
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
