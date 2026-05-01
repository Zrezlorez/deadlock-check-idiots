import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.ActivityService;
import com.litovskiy.service.ConversationStyleService;
import com.litovskiy.service.GrowService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GrowServiceTest {

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
    @DisplayName("Normal growth uses activity bonus and style")
    void growUsesActivityBonusAndConversationStyle() {
        Player player = new Player(77L, 10.0);
        player.setDiscordUserId(22L);

        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 22L)).thenReturn(player);
        when(activityService.getGrowthBonusMultiplier(Platform.DISCORD, 22L, 123L)).thenReturn(1.15);
        when(conversationStyleService.getStyle(Platform.DISCORD, 123L)).thenReturn(GrowthStyle.EMOTIONAL_INTELLIGENCE);
        mockCommonGrowthSettings();
        when(random.nextDouble()).thenReturn(0.5);
        when(random.nextGaussian()).thenReturn(0.0);

        GrowService growService = new GrowService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService,
            random
        );

        String result = growService.grow(Platform.DISCORD, 22L, 123L);

        Assertions.assertFalse(result.isBlank());
        Assertions.assertTrue(player.getSize() > 10.5);
        verify(activityService).getGrowthBonusMultiplier(Platform.DISCORD, 22L, 123L);
        verify(conversationStyleService).getStyle(Platform.DISCORD, 123L);
        verify(playerDao).save(player);
    }

    @Test
    @DisplayName("Critical success increases growth")
    void growSupportsCriticalSuccess() {
        Player player = new Player(77L, 10.0);
        player.setDiscordUserId(22L);

        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 22L)).thenReturn(player);
        when(activityService.getGrowthBonusMultiplier(Platform.DISCORD, 22L, 123L)).thenReturn(1.0);
        when(conversationStyleService.getStyle(Platform.DISCORD, 123L)).thenReturn(GrowthStyle.DICK);
        mockCommonGrowthSettings();
        when(random.nextDouble()).thenReturn(0.2);
        when(random.nextGaussian()).thenReturn(0.0);

        GrowService growService = new GrowService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService,
            random
        );

        String result = growService.grow(Platform.DISCORD, 22L, 123L);

        Assertions.assertFalse(result.isBlank());
        Assertions.assertEquals(10.75, player.getSize(), 0.0001);
        verify(playerDao).save(player);
    }

    @Test
    @DisplayName("Pending modifiers affect and then clear next growth")
    void growConsumesPendingModifiers() {
        Player player = new Player(77L, 10.0);
        player.setDiscordUserId(22L);
        player.setPendingFailChanceBonus(0.2);
        player.setPendingCritChanceBonus(0.3);
        player.setPendingGrowthPenalty(0.25);

        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 22L)).thenReturn(player);
        when(activityService.getGrowthBonusMultiplier(Platform.DISCORD, 22L, 123L)).thenReturn(1.0);
        when(conversationStyleService.getStyle(Platform.DISCORD, 123L)).thenReturn(GrowthStyle.DICK);
        mockCommonGrowthSettings();
        when(random.nextDouble()).thenReturn(0.32);
        when(random.nextGaussian()).thenReturn(0.0);

        GrowService growService = new GrowService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService,
            random
        );

        growService.grow(Platform.DISCORD, 22L, 123L);

        Assertions.assertEquals(10.56, player.getSize(), 0.0001);
        Assertions.assertEquals(0.0, player.getPendingFailChanceBonus(), 0.0001);
        Assertions.assertEquals(0.0, player.getPendingCritChanceBonus(), 0.0001);
        Assertions.assertEquals(0.0, player.getPendingGrowthPenalty(), 0.0001);
    }

    @Test
    @DisplayName("Failure decreases size by configured percent")
    void growSupportsFailure() {
        Player player = new Player(77L, 10.0);
        player.setTelegramChatId(11L);

        when(playerAccountService.resolveOrCreate(Platform.TELEGRAM, 11L)).thenReturn(player);
        when(activityService.getGrowthBonusMultiplier(Platform.TELEGRAM, 11L, -100L)).thenReturn(1.0);
        when(conversationStyleService.getStyle(Platform.TELEGRAM, -100L)).thenReturn(GrowthStyle.DICK);
        mockCommonGrowthSettings();
        when(random.nextDouble()).thenReturn(0.05);

        GrowService growService = new GrowService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService,
            random
        );

        String result = growService.grow(Platform.TELEGRAM, 11L, -100L);

        Assertions.assertFalse(result.isBlank());
        Assertions.assertEquals(9.0, player.getSize(), 0.0001);
        verify(playerDao).save(player);
    }

    @Test
    @DisplayName("Linked profiles share cooldown")
    void growRespectsSharedCooldown() {
        Player player = new Player(77L, 10.0);
        player.setTelegramChatId(11L);
        player.setDiscordUserId(22L);

        when(playerAccountService.resolveOrCreate(eq(Platform.TELEGRAM), eq(11L))).thenReturn(player);
        when(playerAccountService.resolveOrCreate(eq(Platform.DISCORD), eq(22L))).thenReturn(player);
        when(activityService.getGrowthBonusMultiplier(Platform.TELEGRAM, 11L, -100L)).thenReturn(1.0);
        when(conversationStyleService.getStyle(Platform.TELEGRAM, -100L)).thenReturn(GrowthStyle.DICK);
        mockCommonGrowthSettings();
        when(gameConfigService.getInt(GameSetting.COOLDOWN_RANGE)).thenReturn(1);
        when(gameConfigService.getChronoUnit(GameSetting.COOLDOWN_UNIT)).thenReturn(ChronoUnit.SECONDS);
        when(random.nextDouble()).thenReturn(0.5);
        when(random.nextGaussian()).thenReturn(0.0);

        GrowService growService = new GrowService(
            playerDao,
            playerAccountService,
            activityService,
            conversationStyleService,
            gameConfigService,
            random
        );

        String firstGrow = growService.grow(Platform.TELEGRAM, 11L, -100L);
        String secondGrow = growService.grow(Platform.DISCORD, 22L, 200L);

        Assertions.assertFalse(firstGrow.isBlank());
        Assertions.assertFalse(secondGrow.isBlank());
        verify(playerDao, times(1)).save(player);
    }

    private void mockCommonGrowthSettings() {
        lenient().when(gameConfigService.getInt(GameSetting.COOLDOWN_RANGE)).thenReturn(1);
        lenient().when(gameConfigService.getChronoUnit(GameSetting.COOLDOWN_UNIT)).thenReturn(ChronoUnit.HOURS);
        lenient().when(gameConfigService.getDouble(GameSetting.GROWTH_MEAN)).thenReturn(1.05);
        lenient().when(gameConfigService.getDouble(GameSetting.GROWTH_MIN)).thenReturn(1.02);
        lenient().when(gameConfigService.getDouble(GameSetting.GROWTH_MAX)).thenReturn(1.1);
        lenient().when(gameConfigService.getDouble(GameSetting.SLOW_SCALE)).thenReturn(100_000_000.0);
        lenient().when(gameConfigService.getDouble(GameSetting.FAIL_CHANCE)).thenReturn(0.10);
        lenient().when(gameConfigService.getDouble(GameSetting.FAIL_PERCENT)).thenReturn(0.10);
        lenient().when(gameConfigService.getDouble(GameSetting.CRIT_CHANCE)).thenReturn(0.15);
        lenient().when(gameConfigService.getDouble(GameSetting.CRIT_MULTIPLIER)).thenReturn(1.5);
        lenient().when(gameConfigService.getDouble(GameSetting.START_SIZE)).thenReturn(1.0);
    }
}
