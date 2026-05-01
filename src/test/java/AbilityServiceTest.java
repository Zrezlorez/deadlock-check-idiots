import com.litovskiy.dao.ConversationParticipantDao;
import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;
import com.litovskiy.service.AbilityService;
import com.litovskiy.service.ConversationStyleService;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbilityServiceTest {

    @Mock
    private PlayerDao playerDao;

    @Mock
    private PlayerAccountService playerAccountService;

    @Mock
    private ConversationParticipantDao conversationParticipantDao;

    @Mock
    private GameConfigService gameConfigService;

    @Mock
    private ConversationStyleService conversationStyleService;

    @Test
    @DisplayName("Enemy fail chance ability spends size and applies pending fail bonus")
    void increaseEnemyFailChanceAppliesDebuff() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneId.of("Europe/Moscow"));
        AbilityService abilityService = new AbilityService(
            playerDao,
            playerAccountService,
            conversationParticipantDao,
            gameConfigService,
            conversationStyleService,
            clock
        );

        Player actor = new Player(1L, 100.0);
        actor.setTelegramChatId(10L);
        Player target = new Player(2L, 120.0);
        target.setTelegramChatId(20L);

        when(playerAccountService.resolveOrCreate(Platform.TELEGRAM, 10L)).thenReturn(actor);
        when(playerAccountService.resolveOrCreate(Platform.TELEGRAM, 20L)).thenReturn(target);
        when(conversationParticipantDao.exists(2L, Platform.TELEGRAM, -100L)).thenReturn(true);
        when(gameConfigService.getDouble(GameSetting.ENEMY_FAIL_COST_PERCENT)).thenReturn(0.04);
        when(gameConfigService.getDouble(GameSetting.ENEMY_FAIL_CHANCE_BONUS)).thenReturn(0.18);
        when(conversationStyleService.getStyle(Platform.TELEGRAM, -100L)).thenReturn(GrowthStyle.DICK);

        String result = abilityService.increaseEnemyFailChance(Platform.TELEGRAM, 10L, -100L, 20L);

        Assertions.assertTrue(result.contains("18"));
        Assertions.assertEquals(96.0, actor.getSize(), 0.0001);
        Assertions.assertEquals(0.18, target.getPendingFailChanceBonus(), 0.0001);
        Assertions.assertEquals(LocalDateTime.of(2026, 5, 1, 13, 0), actor.getLastAbilityTime());
        verify(playerDao).save(target);
        verify(playerDao).save(actor);
    }

    @Test
    @DisplayName("Self crit ability spends size and buffs next growth")
    void increaseOwnCritChanceAppliesBuff() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneId.of("Europe/Moscow"));
        AbilityService abilityService = new AbilityService(
            playerDao,
            playerAccountService,
            conversationParticipantDao,
            gameConfigService,
            conversationStyleService,
            clock
        );

        Player actor = new Player(1L, 100.0);
        actor.setDiscordUserId(50L);

        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 50L)).thenReturn(actor);
        when(gameConfigService.getDouble(GameSetting.SELF_CRIT_COST_PERCENT)).thenReturn(0.02);
        when(gameConfigService.getDouble(GameSetting.SELF_CRIT_CHANCE_BONUS)).thenReturn(0.25);
        when(conversationStyleService.getStyle(Platform.DISCORD, 777L)).thenReturn(GrowthStyle.DICK);

        String result = abilityService.increaseOwnCritChance(Platform.DISCORD, 777L, 50L);

        Assertions.assertTrue(result.contains("25"));
        Assertions.assertEquals(98.0, actor.getSize(), 0.0001);
        Assertions.assertEquals(0.25, actor.getPendingCritChanceBonus(), 0.0001);
        verify(playerDao).save(actor);
    }

    @Test
    @DisplayName("Free slow ability applies growth penalty to enemy")
    void reduceEnemyGrowthAppliesPenalty() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneId.of("Europe/Moscow"));
        AbilityService abilityService = new AbilityService(
            playerDao,
            playerAccountService,
            conversationParticipantDao,
            gameConfigService,
            conversationStyleService,
            clock
        );

        Player actor = new Player(1L, 100.0);
        actor.setDiscordUserId(10L);
        Player target = new Player(2L, 100.0);
        target.setDiscordUserId(20L);

        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 10L)).thenReturn(actor);
        when(playerAccountService.resolveOrCreate(Platform.DISCORD, 20L)).thenReturn(target);
        when(conversationParticipantDao.exists(2L, Platform.DISCORD, 777L)).thenReturn(true);
        when(gameConfigService.getDouble(GameSetting.ENEMY_GROWTH_PENALTY)).thenReturn(0.25);

        String result = abilityService.reduceEnemyGrowth(Platform.DISCORD, 10L, 777L, 20L);

        Assertions.assertTrue(result.contains("25"));
        Assertions.assertEquals(100.0, actor.getSize(), 0.0001);
        Assertions.assertEquals(0.25, target.getPendingGrowthPenalty(), 0.0001);
        verify(playerDao).save(target);
        verify(playerDao).save(actor);
    }

    @Test
    @DisplayName("Ability cooldown blocks repeated use")
    void abilityCooldownBlocksRepeatedUse() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneId.of("Europe/Moscow"));
        AbilityService abilityService = new AbilityService(
            playerDao,
            playerAccountService,
            conversationParticipantDao,
            gameConfigService,
            conversationStyleService,
            clock
        );

        Player actor = new Player(1L, 100.0);
        actor.setTelegramChatId(10L);
        actor.setLastAbilityTime(LocalDateTime.of(2026, 5, 1, 12, 0));

        when(playerAccountService.resolveOrCreate(Platform.TELEGRAM, 10L)).thenReturn(actor);
        when(gameConfigService.getInt(GameSetting.ABILITY_COOLDOWN_RANGE)).thenReturn(8);
        when(gameConfigService.getChronoUnit(GameSetting.ABILITY_COOLDOWN_UNIT)).thenReturn(ChronoUnit.HOURS);

        String result = abilityService.increaseOwnCritChance(Platform.TELEGRAM, -100L, 10L);

        Assertions.assertTrue(result.contains("перезарядке"));
        verify(playerDao, never()).save(actor);
    }
}
