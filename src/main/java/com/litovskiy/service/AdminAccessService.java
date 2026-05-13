package com.litovskiy.service;

import com.litovskiy.entity.Platform;
import com.litovskiy.util.PropsManager;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminAccessService {

    private final Set<Long> telegramAdminIds;
    private final Set<Long> discordAdminIds;

    public AdminAccessService() {
        this.telegramAdminIds = parseIds(PropsManager.getProps().getProperty("admin.telegramUserIds", ""));
        this.discordAdminIds = parseIds(PropsManager.getProps().getProperty("admin.discordUserIds", ""));
    }

    public boolean isAdmin(Platform platform, long profileId) {
        return switch (platform) {
            case TELEGRAM -> telegramAdminIds.contains(profileId);
            case DISCORD -> discordAdminIds.contains(profileId);
        };
    }

    public String describeConfiguration() {
        return "Куда мы лезем?";
    }

    private Set<Long> parseIds(String rawValue) {
        return Arrays.stream(rawValue.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(Long::parseLong)
            .collect(Collectors.toSet());
    }
}
