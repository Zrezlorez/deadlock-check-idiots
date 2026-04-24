import com.litovskiy.dao.ActivityStatDao;
import com.litovskiy.dao.VoiceSessionDao;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.entity.VoiceSession;
import com.litovskiy.service.ActivityService;
import com.litovskiy.service.GameConfigService;
import com.litovskiy.service.GameSetting;
import com.litovskiy.service.PlayerAccountService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ActivityServiceTest {

    @Mock
    private PlayerAccountService playerAccountService;

    @Mock
    private ActivityStatDao activityStatDao;

    @Mock
    private VoiceSessionDao voiceSessionDao;

    @Mock
    private GameConfigService gameConfigService;

    @Test
    @DisplayName("Бонус суммирует вклад Telegram и Discord")
    void growthBonusSumsTelegramAndDiscordActivity() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-25T12:00:00Z"), ZoneId.of("Europe/Moscow"));
        ActivityService activityService = new ActivityService(
            playerAccountService,
            activityStatDao,
            voiceSessionDao,
            gameConfigService,
            clock
        );

        Player player = new Player(10L, 5.0);
        when(playerAccountService.resolveOrCreate(Platform.TELEGRAM, 100L)).thenReturn(player);
        when(gameConfigService.getInt(GameSetting.ACTIVITY_LOOKBACK_DAYS)).thenReturn(7);
        when(gameConfigService.getDouble(GameSetting.ACTIVITY_MAX_GROWTH_BONUS)).thenReturn(0.15);
        when(activityStatDao.findMessageTotals(Platform.TELEGRAM, -100L, LocalDate.of(2026, 4, 19)))
            .thenReturn(Map.of(10L, 15L, 20L, 30L));
        when(activityStatDao.findMessageTotals(Platform.DISCORD, 555L, LocalDate.of(2026, 4, 19)))
            .thenReturn(Map.of(10L, 10L, 20L, 10L));
        when(activityStatDao.findVoiceTotals(Platform.DISCORD, 555L, LocalDate.of(2026, 4, 19)))
            .thenReturn(Map.of(20L, 300L));
        when(voiceSessionDao.findActiveSessions(Platform.DISCORD, 555L))
            .thenReturn(List.of(new VoiceSession(10L, Platform.DISCORD, 555L, LocalDateTime.of(2026, 4, 25, 14, 30))));
        when(activityStatDao.findScopeIdsByPlayer(10L, Platform.DISCORD, LocalDate.of(2026, 4, 19)))
            .thenReturn(List.of(555L));
        when(voiceSessionDao.findActiveScopeIdsByPlayer(10L, Platform.DISCORD))
            .thenReturn(List.of(555L));

        double multiplier = activityService.getGrowthBonusMultiplier(Platform.TELEGRAM, 100L, -100L);

        Assertions.assertEquals(1.225, multiplier, 0.0001);
    }

    @Test
    @DisplayName("Без текущего scope бонус берет лучший scope платформы")
    void growthBonusUsesBestKnownScopeWithoutCurrentScope() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-25T12:00:00Z"), ZoneId.of("Europe/Moscow"));
        ActivityService activityService = new ActivityService(
            playerAccountService,
            activityStatDao,
            voiceSessionDao,
            gameConfigService,
            clock
        );

        Player player = new Player(10L, 5.0);
        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 200L)).thenReturn(player);
        when(gameConfigService.getInt(GameSetting.ACTIVITY_LOOKBACK_DAYS)).thenReturn(7);
        when(gameConfigService.getDouble(GameSetting.ACTIVITY_MAX_GROWTH_BONUS)).thenReturn(0.15);
        when(activityStatDao.findScopeIdsByPlayer(10L, Platform.TELEGRAM, LocalDate.of(2026, 4, 19)))
            .thenReturn(List.of(-100L, -200L));
        when(activityStatDao.findMessageTotals(Platform.TELEGRAM, -100L, LocalDate.of(2026, 4, 19)))
            .thenReturn(Map.of(10L, 5L, 20L, 10L));
        when(activityStatDao.findMessageTotals(Platform.TELEGRAM, -200L, LocalDate.of(2026, 4, 19)))
            .thenReturn(Map.of(10L, 20L, 20L, 20L));
        when(activityStatDao.findScopeIdsByPlayer(10L, Platform.DISCORD, LocalDate.of(2026, 4, 19)))
            .thenReturn(List.of());
        when(voiceSessionDao.findActiveScopeIdsByPlayer(10L, Platform.DISCORD))
            .thenReturn(List.of());

        double multiplier = activityService.getGrowthBonusMultiplier(Platform.DISCORD, 200L, null);

        Assertions.assertEquals(1.15, multiplier, 0.0001);
    }

    @Test
    @DisplayName("Сообщение увеличивает счетчик активности")
    void recordMessageIncrementsStats() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-25T12:00:00Z"), ZoneId.of("Europe/Moscow"));
        ActivityService activityService = new ActivityService(
            playerAccountService,
            activityStatDao,
            voiceSessionDao,
            gameConfigService,
            clock
        );

        Player player = new Player(10L, 5.0);
        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 200L)).thenReturn(player);

        activityService.recordMessage(Platform.DISCORD, 200L, 555L);

        verify(activityStatDao).incrementMessages(10L, Platform.DISCORD, 555L, LocalDate.of(2026, 4, 25), 1);
    }
}
