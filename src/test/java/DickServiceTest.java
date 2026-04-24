import com.litovskiy.dao.GenericDao;
import com.litovskiy.entity.Player;
import com.litovskiy.service.DickService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DickServiceTest {

    private static final Pattern SUCCESS_PATTERN = Pattern.compile(
        "Ваш член вырос на (\\d+\\.\\d+) (см|м|км|к км)\\. Текущий размер: (\\d+\\.\\d+) (см|м|км|к км)"
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Mock
    private GenericDao<Player> playerDao;

    @InjectMocks
    private DickService dickService;

    @Test
    @DisplayName("Проверка успешного роста")
    public void testSuccessfulGrow() {
        when(playerDao.find(1)).thenReturn(new Player(1L, 1.0));

        String result = dickService.grow(1);
        Matcher matcher = SUCCESS_PATTERN.matcher(result);

        System.out.println(result);
        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals("см", matcher.group(2));
        Assertions.assertEquals("см", matcher.group(4));

        double range = Double.parseDouble(matcher.group(1));
        double newSize = Double.parseDouble(matcher.group(3));
        Assertions.assertTrue(range < 1.0 && range > 0.0);
        Assertions.assertTrue(newSize > 1.0 && newSize < 2.0);
    }

    @Test
    @DisplayName("Есть кд роста")
    public void testCooldownGrow() {
        AtomicReference<Player> storage = new AtomicReference<>(new Player(1L, 1.0));
        mockSinglePlayerStorage(storage);

        String firstResult = dickService.grow(1);
        Assertions.assertTrue(isSuccessfulGrow(firstResult));

        LocalDateTime savedGrowTime = storage.get().getLastGrowTime();
        Assertions.assertNotEquals(LocalDateTime.MIN, savedGrowTime);

        String secondResult = dickService.grow(1);
        Assertions.assertFalse(isSuccessfulGrow(secondResult));
        Assertions.assertNotEquals(firstResult, secondResult);
        Assertions.assertTrue(secondResult.contains(savedGrowTime.plusSeconds(1).format(TIME_FORMATTER)));

        verify(playerDao, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Разный рост у двух игроков")
    public void testPairGrow() {
        when(playerDao.find(1)).thenReturn(new Player(1L, 1.0));
        when(playerDao.find(2)).thenReturn(new Player(2L, 2.0));

        String result1 = dickService.grow(1);
        String result2 = dickService.grow(2);
        Matcher matcher1 = SUCCESS_PATTERN.matcher(result1);
        Matcher matcher2 = SUCCESS_PATTERN.matcher(result2);

        Assertions.assertTrue(matcher1.find());
        Assertions.assertTrue(matcher2.find());
        Assertions.assertNotEquals(Double.parseDouble(matcher1.group(3)), Double.parseDouble(matcher2.group(3)));
    }

    @Test
    @DisplayName("Разное кд у двух игроков")
    public void testPairCooldownSuccessfulGrow() {
        Map<Long, Player> storage = new HashMap<>();
        storage.put(1L, new Player(1L, 1.0));
        storage.put(2L, new Player(2L, 2.0));
        mockPlayersStorage(storage);

        String firstPlayerFirstGrow = dickService.grow(1);
        Assertions.assertTrue(isSuccessfulGrow(firstPlayerFirstGrow));

        LocalDateTime firstPlayerSavedGrowTime = storage.get(1L).getLastGrowTime();
        Assertions.assertNotEquals(LocalDateTime.MIN, firstPlayerSavedGrowTime);

        String firstPlayerSecondGrow = dickService.grow(1);
        Assertions.assertFalse(isSuccessfulGrow(firstPlayerSecondGrow));
        Assertions.assertTrue(firstPlayerSecondGrow.contains(firstPlayerSavedGrowTime.plusSeconds(1).format(TIME_FORMATTER)));

        String secondPlayerGrow = dickService.grow(2);
        Assertions.assertTrue(isSuccessfulGrow(secondPlayerGrow));
        Assertions.assertNotEquals(LocalDateTime.MIN, storage.get(2L).getLastGrowTime());

        verify(playerDao, times(2)).save(any(Player.class));
    }

    private void mockSinglePlayerStorage(AtomicReference<Player> storage) {
        when(playerDao.find(1)).thenAnswer(invocation -> copyPlayer(storage.get()));
        doAnswer(invocation -> {
            Player savedPlayer = invocation.getArgument(0);
            storage.set(copyPlayer(savedPlayer));
            return null;
        }).when(playerDao).save(any(Player.class));
    }

    private void mockPlayersStorage(Map<Long, Player> storage) {
        when(playerDao.find(anyLong())).thenAnswer(invocation -> copyPlayer(storage.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            Player savedPlayer = invocation.getArgument(0);
            storage.put(savedPlayer.getChatId(), copyPlayer(savedPlayer));
            return null;
        }).when(playerDao).save(any(Player.class));
    }

    private boolean isSuccessfulGrow(String result) {
        return SUCCESS_PATTERN.matcher(result).find();
    }

    private Player copyPlayer(Player source) {
        if (source == null) {
            return null;
        }

        Player copy = new Player(source.getChatId(), source.getSize());
        copy.setLastGrowTime(source.getLastGrowTime());
        return copy;
    }
}
