package com.litovskiy.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SettingGroup {

    GROWTH("🌱 Рост"),
    ABILITIES("⚡ Способности"),
    ENEMY_ABILITIES("🎯 Способности на врага"),
    SELF_ABILITIES("🛡 Способности на себя"),
    LIMITS("📏 Лимиты"),
    OTHER("⚙️ Остальное");

    private final String displayName;
}