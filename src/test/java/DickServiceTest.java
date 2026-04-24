import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.ActivityService;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.DickService;
import com.litovskiy.service.GameConfigService;
import com.litovskiy.service.GameSetting;
import com.litovskiy.service.PlayerAccountService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.temporal.ChronoUnit;
import java.util.Random;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DickServiceTest {

    @Mock
    private PlayerDao playerDao;

    @Mock
    private PlayerAccountService playerAccountService;

    @Mock
    private ActivityService activityService;

    @Mock
    private ConversationStyleService conversationStyleService;

    @Mock
    private GameConfigService gameConfigService;

    @Mock
    private Random random;

    @Test
    @DisplayName("Рост учитывает бонус активности и стиль беседы")
    void growUsesActivityBonusAndConversationStyle() {
        Player player = new Player(77L, 10.0);
        player.setDiscordUserId(22L);

        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 22L)).thenReturn(player);
        when(activityService.getGrowthBonusMultiplier(Platform.DISCORD, 22L, 123L)).thenReturn(1.15);
        when(conversationStyleService.getStyle(Platform.DISCORD, 123L)).thenReturn(GrowthStyle.EMOTIONAL_INTELLIGENCE);
        when(gameConfigService.getDouble(GameSetting.GROWTH_MEAN)).thenReturn(1.05);
        when(gameConfigService.getDouble(GameSetting.GROWTH_MIN)).thenReturn(1.02);
        when(gameConfigService.getDouble(GameSetting.GROWTH_MAX)).thenReturn(1.1);
        when(gameConfigService.getDouble(GameSetting.SLOW_SCALE)).thenReturn(100_000_000.0);
        when(random.nextGaussian()).thenReturn(0.0);

        DickService dickService = new DickService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService,
            random
        );

        String result = dickService.grow(Platform.DISCORD, 22L, 123L);

        Assertions.assertTrue(result.contains("эмоциональный интеллект"));
        Assertions.assertTrue(player.getSize() > 10.5);
        verify(activityService).getGrowthBonusMultiplier(Platform.DISCORD, 22L, 123L);
        verify(conversationStyleService).getStyle(Platform.DISCORD, 123L);
        verify(playerDao).save(player);
    }

    @Test
    @DisplayName("Кулдаун общий для привязанных профилей")
    void growRespectsSharedCooldown() {
        Player player = new Player(77L, 10.0);
        player.setTelegramChatId(11L);
        player.setDiscordUserId(22L);

        when(playerAccountService.resolveOrCreate(eq(Platform.TELEGRAM), eq(11L))).thenReturn(player);
        when(playerAccountService.resolveOrCreate(eq(Platform.DISCORD), eq(22L))).thenReturn(player);
        when(activityService.getGrowthBonusMultiplier(Platform.TELEGRAM, 11L, -100L)).thenReturn(1.0);
        when(conversationStyleService.getStyle(Platform.TELEGRAM, -100L)).thenReturn(GrowthStyle.DICK);
        when(gameConfigService.getInt(GameSetting.COOLDOWN_RANGE)).thenReturn(1);
        when(gameConfigService.getChronoUnit(GameSetting.COOLDOWN_UNIT)).thenReturn(ChronoUnit.SECONDS);
        when(gameConfigService.getDouble(GameSetting.GROWTH_MEAN)).thenReturn(1.05);
        when(gameConfigService.getDouble(GameSetting.GROWTH_MIN)).thenReturn(1.02);
        when(gameConfigService.getDouble(GameSetting.GROWTH_MAX)).thenReturn(1.1);
        when(gameConfigService.getDouble(GameSetting.SLOW_SCALE)).thenReturn(100_000_000.0);
        when(random.nextGaussian()).thenReturn(0.0);

        DickService dickService = new DickService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService,
            random
        );

        String firstGrow = dickService.grow(Platform.TELEGRAM, 11L, -100L);
        String secondGrow = dickService.grow(Platform.DISCORD, 22L, 200L);

        Assertions.assertTrue(firstGrow.contains("Ваш член вырос на"));
        Assertions.assertTrue(secondGrow.contains("следующая попытка будет в"));
        verify(playerDao, times(1)).save(player);
    }
}
