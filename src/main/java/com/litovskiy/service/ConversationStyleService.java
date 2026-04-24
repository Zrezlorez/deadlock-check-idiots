package com.litovskiy.service;

import com.litovskiy.dao.ConversationSettingsDao;
import com.litovskiy.entity.ConversationSettings;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;

public class ConversationStyleService {

    private final ConversationSettingsDao conversationSettingsDao;

    public ConversationStyleService(ConversationSettingsDao conversationSettingsDao) {
        this.conversationSettingsDao = conversationSettingsDao;
    }

    public void registerTelegramManager(long scopeId, long managerProfileId) {
        ConversationSettings settings = getOrCreate(Platform.TELEGRAM, scopeId);
        settings.setManagerProfileId(managerProfileId);
        conversationSettingsDao.save(settings);
    }

    public GrowthStyle getStyle(Platform platform, Long scopeId) {
        if (scopeId == null) {
            return GrowthStyle.DICK;
        }

        ConversationSettings settings = conversationSettingsDao.findByScope(platform, scopeId);
        return settings == null ? GrowthStyle.DICK : settings.getGrowthStyle();
    }

    public String describeCurrentStyle(Platform platform, long scopeId) {
        GrowthStyle style = getStyle(platform, scopeId);
        return "Текущий стиль: " + style.key() + " (" + style.displayName() + ")\n"
            + "Доступные стили: " + GrowthStyle.availableStyles();
    }

    public String updateTelegramStyle(long scopeId, long requesterProfileId, String styleKey) {
        GrowthStyle style = GrowthStyle.fromKey(styleKey);
        if (style == null) {
            return "Неизвестный стиль. Доступные стили: " + GrowthStyle.availableStyles();
        }

        ConversationSettings settings = getOrCreate(Platform.TELEGRAM, scopeId);
        if (settings.getManagerProfileId() == null) {
            return "Я не знаю, кто добавил меня в эту группу. Удалите и добавьте бота заново, чтобы закрепить владельца стиля.";
        }

        if (!settings.getManagerProfileId().equals(requesterProfileId)) {
            return "Менять стиль в этой группе может только тот, кто добавил бота.";
        }

        settings.setGrowthStyle(style);
        conversationSettingsDao.save(settings);
        return "Стиль группы изменен: " + style.displayName() + ".";
    }

    public String updateDiscordStyle(long scopeId, String styleKey) {
        GrowthStyle style = GrowthStyle.fromKey(styleKey);
        if (style == null) {
            return "Неизвестный стиль. Доступные стили: " + GrowthStyle.availableStyles();
        }

        ConversationSettings settings = getOrCreate(Platform.DISCORD, scopeId);
        settings.setGrowthStyle(style);
        conversationSettingsDao.save(settings);
        return "Стиль сервера изменен: " + style.displayName() + ".";
    }

    private ConversationSettings getOrCreate(Platform platform, long scopeId) {
        ConversationSettings settings = conversationSettingsDao.findByScope(platform, scopeId);
        return settings == null ? new ConversationSettings(platform, scopeId) : settings;
    }
}
