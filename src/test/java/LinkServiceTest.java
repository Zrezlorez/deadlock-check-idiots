import com.litovskiy.dao.LinkCodeDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.LinkCode;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.GameConfigService;
import com.litovskiy.service.GameSetting;
import com.litovskiy.service.LinkService;
import com.litovskiy.service.PlayerAccountService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LinkServiceTest {

    @Mock
    private PlayerDao playerDao;

    @Mock
    private LinkCodeDao linkCodeDao;

    @Mock
    private PlayerAccountService playerAccountService;

    @Mock
    private GameConfigService gameConfigService;

    @Test
    @DisplayName("Создание кода сохраняет код привязки для аккаунта")
    void createCodePersistsFreshCode() {
        Player player = new Player(10L, 5.0);
        player.setTelegramChatId(100L);

        SecureRandom random = new FixedSecureRandom(0, 1, 2, 3, 4, 5);
        LinkService linkService = new LinkService(playerDao, linkCodeDao, playerAccountService, gameConfigService, random);

        when(playerAccountService.resolveOrCreate(Platform.TELEGRAM, 100L)).thenReturn(player);
        when(gameConfigService.getInt(GameSetting.LINK_CODE_LENGTH)).thenReturn(6);
        when(gameConfigService.getInt(GameSetting.LINK_CODE_LIFETIME_MINUTES)).thenReturn(10);

        String result = linkService.createCode(Platform.TELEGRAM, 100L);

        ArgumentCaptor<LinkCode> captor = ArgumentCaptor.forClass(LinkCode.class);
        verify(linkCodeDao).findByPlayerChatId(10L);
        verify(linkCodeDao).save(captor.capture());

        LinkCode savedCode = captor.getValue();
        Assertions.assertEquals("ABCDEF", savedCode.getCode());
        Assertions.assertEquals(10L, savedCode.getPlayerChatId());
        Assertions.assertEquals(Platform.TELEGRAM, savedCode.getSourcePlatform());
        Assertions.assertTrue(result.contains("Код привязки: ABCDEF"));
    }

    @Test
    @DisplayName("Повторный запрос возвращает существующий активный код")
    void createCodeReturnsExistingActiveCode() {
        Player player = new Player(10L, 5.0);
        player.setTelegramChatId(100L);
        LinkCode existingCode = new LinkCode("ZZZZZZ", 10L, Platform.TELEGRAM, LocalDateTime.now().plusMinutes(5));

        LinkService linkService = new LinkService(playerDao, linkCodeDao, playerAccountService, gameConfigService);

        when(playerAccountService.resolveOrCreate(Platform.TELEGRAM, 100L)).thenReturn(player);
        when(linkCodeDao.findByPlayerChatId(10L)).thenReturn(existingCode);

        String result = linkService.createCode(Platform.TELEGRAM, 100L);

        Assertions.assertTrue(result.contains("У вас уже есть активный код привязки."));
        Assertions.assertTrue(result.contains("Код привязки: ZZZZZZ"));
        verify(linkCodeDao).findByPlayerChatId(10L);
        verify(linkCodeDao, never()).save(any());
    }

    @Test
    @DisplayName("Привязка объединяет профили в один аккаунт")
    void linkProfileMergesAccounts() {
        Player targetPlayer = new Player(10L, 5.0);
        targetPlayer.setTelegramChatId(100L);
        targetPlayer.setLastGrowTime(LocalDateTime.now().minusMinutes(5));

        Player currentPlayer = new Player(20L, 8.0);
        currentPlayer.setDiscordUserId(200L);
        currentPlayer.setLastGrowTime(LocalDateTime.now().minusMinutes(1));

        LinkCode linkCode = new LinkCode("ABCDEF", 10L, Platform.TELEGRAM, LocalDateTime.now().plusMinutes(10));

        LinkService linkService = new LinkService(playerDao, linkCodeDao, playerAccountService, gameConfigService);

        when(linkCodeDao.findByCode("ABCDEF")).thenReturn(linkCode);
        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 200L)).thenReturn(currentPlayer);
        when(playerDao.find(10L)).thenReturn(targetPlayer);

        String result = linkService.linkProfile(Platform.DISCORD, 200L, "abcdef");

        Assertions.assertEquals("Профили объединены. Теперь Telegram и Discord используют один аккаунт.", result);
        Assertions.assertEquals(100L, targetPlayer.getTelegramChatId());
        Assertions.assertEquals(200L, targetPlayer.getDiscordUserId());
        Assertions.assertEquals(8.0, targetPlayer.getSize());
        Assertions.assertEquals(currentPlayer.getLastGrowTime(), targetPlayer.getLastGrowTime());
        verify(playerDao).mergeAndDeleteSource(targetPlayer, currentPlayer);
        verify(linkCodeDao).delete(linkCode);
    }

    @Test
    @DisplayName("Код нельзя использовать в том же боте")
    void linkProfileRejectsSamePlatformCode() {
        LinkCode linkCode = new LinkCode("ABCDEF", 10L, Platform.DISCORD, LocalDateTime.now().plusMinutes(10));
        LinkService linkService = new LinkService(playerDao, linkCodeDao, playerAccountService, gameConfigService);

        when(linkCodeDao.findByCode("ABCDEF")).thenReturn(linkCode);

        String result = linkService.linkProfile(Platform.DISCORD, 200L, "ABCDEF");

        Assertions.assertEquals("Этот код нужно вводить в другом боте.", result);
        verify(playerAccountService, never()).resolveOrCreate(any(), eq(200L));
        verify(playerDao, never()).save(any());
    }

    private static class FixedSecureRandom extends SecureRandom {
        private final int[] values;
        private int index;

        private FixedSecureRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[index % values.length];
            index++;
            return value % bound;
        }
    }
}
