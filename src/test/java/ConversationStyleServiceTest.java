import com.litovskiy.dao.ConversationSettingsDao;
import com.litovskiy.entity.ConversationSettings;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.service.ConversationStyleService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConversationStyleServiceTest {

    @Mock
    private ConversationSettingsDao conversationSettingsDao;

    @Test
    @DisplayName("Стиль Telegram может менять только добавивший бота")
    void telegramStyleRequiresRecordedManager() {
        ConversationStyleService service = new ConversationStyleService(conversationSettingsDao);
        ConversationSettings settings = new ConversationSettings(Platform.TELEGRAM, -100L);
        settings.setManagerProfileId(42L);

        when(conversationSettingsDao.findByScope(Platform.TELEGRAM, -100L)).thenReturn(settings);

        String result = service.updateTelegramStyle(-100L, 99L, "emotional_intelligence");

        Assertions.assertEquals("Менять стиль в этой группе может только тот, кто добавил бота.", result);
    }

    @Test
    @DisplayName("Изменение стиля сохраняет новый стиль беседы")
    void updateDiscordStylePersistsSelectedStyle() {
        ConversationStyleService service = new ConversationStyleService(conversationSettingsDao);

        String result = service.updateDiscordStyle(555L, "emotional_intelligence");

        ArgumentCaptor<ConversationSettings> captor = ArgumentCaptor.forClass(ConversationSettings.class);
        verify(conversationSettingsDao).save(captor.capture());

        Assertions.assertEquals("Стиль сервера изменен: эмоциональный интеллект.", result);
        Assertions.assertEquals(Platform.DISCORD, captor.getValue().getPlatform());
        Assertions.assertEquals(555L, captor.getValue().getScopeId());
        Assertions.assertEquals(GrowthStyle.EMOTIONAL_INTELLIGENCE, captor.getValue().getGrowthStyle());
    }
}
