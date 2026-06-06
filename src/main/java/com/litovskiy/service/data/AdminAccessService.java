package com.litovskiy.service.data;

import com.litovskiy.config.properties.AdminProperties;
import com.litovskiy.entity.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAccessService {

    private final AdminProperties adminProperties;

    public boolean isAdmin(Platform platform, long profileId) {
        return switch (platform) {
            case TELEGRAM -> adminProperties.getTelegramAdminIds().contains(profileId);
            case DISCORD -> adminProperties.getDiscordAdminIds().contains(profileId);
        };
    }

    public String describeConfiguration() {
        return "Куда мы лезем?";
    }
}
