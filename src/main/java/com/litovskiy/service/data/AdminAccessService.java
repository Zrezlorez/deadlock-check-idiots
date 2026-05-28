package com.litovskiy.service.data;

import com.litovskiy.entity.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdminAccessService {

    @Value("${admin.telegramUserIds}")
    private Set<Long> telegramAdminIds;
    @Value("${admin.discordUserIds}")
    private Set<Long> discordAdminIds;

    public boolean isAdmin(Platform platform, long profileId) {
        return switch (platform) {
            case TELEGRAM -> telegramAdminIds.contains(profileId);
            case DISCORD -> discordAdminIds.contains(profileId);
        };
    }

    public String describeConfiguration() {
        return "Куда мы лезем?";
    }
}
