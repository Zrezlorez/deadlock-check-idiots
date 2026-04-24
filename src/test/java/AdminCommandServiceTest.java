import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.AdminAccessService;
import com.litovskiy.service.AdminCommandService;
import com.litovskiy.service.GameConfigService;
import com.litovskiy.service.GameSetting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminCommandServiceTest {

    @Mock
    private AdminAccessService adminAccessService;

    @Mock
    private GameConfigService gameConfigService;

    @Mock
    private PlayerDao playerDao;

    @Test
    @DisplayName("admin config показывает текущие значения")
    void configCommandShowsSettings() {
        AdminCommandService service = new AdminCommandService(adminAccessService, gameConfigService, playerDao);
        Map<GameSetting, String> values = new LinkedHashMap<>();
        values.put(GameSetting.START_SIZE, "1.0");
        values.put(GameSetting.GROWTH_MEAN, "1.15");

        when(adminAccessService.isAdmin(Platform.TELEGRAM, 1L)).thenReturn(true);
        when(gameConfigService.listEffectiveValues()).thenReturn(values);

        String result = service.handle(Platform.TELEGRAM, 1L, "config");

        Assertions.assertTrue(result.contains("start_size = 1.0"));
        Assertions.assertTrue(result.contains("growth_mean = 1.15"));
    }

    @Test
    @DisplayName("admin player set-size меняет размер игрока")
    void playerSetSizeUpdatesPlayer() {
        AdminCommandService service = new AdminCommandService(adminAccessService, gameConfigService, playerDao);
        Player player = new Player(10L, 5.0);
        player.setLastGrowTime(LocalDateTime.now());

        when(adminAccessService.isAdmin(Platform.DISCORD, 2L)).thenReturn(true);
        when(playerDao.findByPlatform(Platform.TELEGRAM, 100L)).thenReturn(player);

        String result = service.handle(Platform.DISCORD, 2L, "player set-size telegram 100 42.5");

        Assertions.assertTrue(result.contains("42.5"));
        verify(playerDao).save(player);
    }
}
