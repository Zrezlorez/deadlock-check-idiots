package com.litovskiy.entity;

import java.util.Arrays;

public enum GrowthStyle {
    DICK("dick", "член"),
    EMOTIONAL_INTELLIGENCE("emotional_intelligence", "эмоциональный интеллект");

    private final String key;
    private final String displayName;

    GrowthStyle(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public static GrowthStyle fromKey(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String normalized = rawValue.trim().toLowerCase();
        return Arrays.stream(values())
            .filter(style -> style.key.equals(normalized))
            .findFirst()
            .orElse(null);
    }

    public static String availableStyles() {
        return Arrays.stream(values())
            .map(style -> style.key + " (" + style.displayName + ")")
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }
}
