import com.litovskiy.dao.ConversationParticipantDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.GameConfigService;
import com.litovskiy.service.GameSetting;
import com.litovskiy.service.LeaderboardService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LeaderboardServiceTest {

    @Mock
    private PlayerDao playerDao;

    @Mock
    private ConversationParticipantDao conversationParticipantDao;

    @Mock
    private ConversationStyleService conversationStyleService;

    @Mock
    private GameConfigService gameConfigService;

    @Test
    @DisplayName("Telegram leaderboard uses profile links")
    void buildScopeLeaderboardFormatsTelegramProfileLink() {
        Player first = new Player(1L, 350.0);
        first.setTelegramChatId(1001L);
        first.setTelegramDisplayName("Ivan");
        Player second = new Player(2L, 120.0);
        second.setTelegramChatId(1002L);
        second.setTelegramDisplayName("Petr");

        LeaderboardService leaderboardService = new LeaderboardService(
            playerDao,
            conversationParticipantDao,
            conversationStyleService,
            gameConfigService
        );

        when(gameConfigService.getInt(GameSetting.LEADERBOARD_LIMIT)).thenReturn(10);

        when(playerDao.findTopByPlatform(Platform.TELEGRAM, 10)).thenReturn(List.of(second, first));
        when(conversationStyleService.getStyle(Platform.TELEGRAM, null)).thenReturn(GrowthStyle.DICK);

        String result = leaderboardService.buildLeaderboard(Platform.TELEGRAM, 1002L, null);

        Assertions.assertTrue(result.contains("tg://user?id=1001"));
        Assertions.assertTrue(result.contains(">Ivan</a>"));
        Assertions.assertTrue(result.contains("tg://user?id=1002"));
        Assertions.assertTrue(result.contains(">Petr</a>"));
    }

    @Test
    @DisplayName("Discord leaderboard keeps mentions")
    void buildGlobalLeaderboardKeepsDiscordMentions() {
        Player first = new Player(1L, 500.0);
        first.setDiscordUserId(11L);
        Player second = new Player(2L, 300.0);
        second.setDiscordUserId(22L);

        LeaderboardService leaderboardService = new LeaderboardService(
            playerDao,
            conversationParticipantDao,
            conversationStyleService,
            gameConfigService
        );

        when(gameConfigService.getInt(GameSetting.LEADERBOARD_LIMIT)).thenReturn(2);
        when(playerDao.findTopByPlatform(Platform.DISCORD, 2)).thenReturn(List.of(first, second));
        when(conversationStyleService.getStyle(Platform.DISCORD, null)).thenReturn(GrowthStyle.EMOTIONAL_INTELLIGENCE);

        String result = leaderboardService.buildLeaderboard(Platform.DISCORD, 33L, null);

        Assertions.assertTrue(result.contains("<@11>"));
        Assertions.assertTrue(result.contains("<@22>"));
    }

    @Test
    @DisplayName("Scoped leaderboard uses registered participants instead of activity stats")
    void buildScopeLeaderboardUsesConversationParticipants() {
        Player first = new Player(10L, 500.0);
        first.setDiscordUserId(101L);
        Player second = new Player(20L, 100.0);
        second.setDiscordUserId(202L);

        LeaderboardService leaderboardService = new LeaderboardService(
            playerDao,
            conversationParticipantDao,
            conversationStyleService,
            gameConfigService
        );

        when(gameConfigService.getInt(GameSetting.LEADERBOARD_LIMIT)).thenReturn(10);
        when(conversationParticipantDao.findParticipantIds(Platform.DISCORD, 777L)).thenReturn(List.of(20L, 10L));
        when(playerDao.findByChatIds(List.of(20L, 10L))).thenReturn(List.of(second, first));
        when(conversationStyleService.getStyle(Platform.DISCORD, 777L)).thenReturn(GrowthStyle.DICK);

        String result = leaderboardService.buildLeaderboard(Platform.DISCORD, 202L, 777L);

        Assertions.assertTrue(result.contains("<@101>"));
        Assertions.assertTrue(result.contains("<@202>"));
        Assertions.assertTrue(result.contains("← вы"));
    }
}
