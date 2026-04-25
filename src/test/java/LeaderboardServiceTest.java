import com.litovskiy.dao.ActivityStatDao;
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
    private ActivityStatDao activityStatDao;

    @Mock
    private ConversationStyleService conversationStyleService;

    @Mock
    private GameConfigService gameConfigService;

    @Test
    @DisplayName("Локальный лидерборд сортирует участников беседы по размеру")
    void buildScopeLeaderboardSortsParticipants() {
        Player first = new Player(1L, 350.0);
        first.setTelegramChatId(1001L);
        Player second = new Player(2L, 120.0);
        second.setTelegramChatId(1002L);

        LeaderboardService leaderboardService = new LeaderboardService(
            playerDao,
            activityStatDao,
            conversationStyleService,
            gameConfigService
        );

        when(gameConfigService.getInt(GameSetting.LEADERBOARD_LIMIT)).thenReturn(10);
        when(activityStatDao.findParticipantIds(Platform.TELEGRAM, -100L)).thenReturn(List.of(1L, 2L));
        when(playerDao.findByChatIds(List.of(1L, 2L))).thenReturn(List.of(second, first));
        when(conversationStyleService.getStyle(Platform.TELEGRAM, -100L)).thenReturn(GrowthStyle.DICK);

        String result = leaderboardService.buildLeaderboard(Platform.TELEGRAM, 1002L, -100L);

        Assertions.assertTrue(result.contains("Топ этой беседы"));
        Assertions.assertTrue(result.indexOf("id 1001") < result.indexOf("id 1002"));
        Assertions.assertTrue(result.contains("← вы"));
    }

    @Test
    @DisplayName("Глобальный лидерборд показывает место игрока вне видимого топа")
    void buildGlobalLeaderboardShowsRequesterPlace() {
        Player first = new Player(1L, 500.0);
        first.setDiscordUserId(11L);
        Player second = new Player(2L, 300.0);
        second.setDiscordUserId(22L);
        Player third = new Player(3L, 100.0);
        third.setDiscordUserId(33L);

        LeaderboardService leaderboardService = new LeaderboardService(
            playerDao,
            activityStatDao,
            conversationStyleService,
            gameConfigService
        );

        when(gameConfigService.getInt(GameSetting.LEADERBOARD_LIMIT)).thenReturn(2);
        when(playerDao.findTopByPlatform(Platform.DISCORD, 2)).thenReturn(List.of(first, second));
        when(conversationStyleService.getStyle(Platform.DISCORD, null)).thenReturn(GrowthStyle.EMOTIONAL_INTELLIGENCE);

        String result = leaderboardService.buildLeaderboard(Platform.DISCORD, 33L, null);

        Assertions.assertTrue(result.contains("Глобальный топ"));
        Assertions.assertFalse(result.contains("<@33>"));
    }
}
