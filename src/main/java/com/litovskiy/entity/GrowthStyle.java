package com.litovskiy.entity;

import lombok.Getter;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import static com.litovskiy.util.StringUtil.round;

@Getter
public enum GrowthStyle {
    DICK("dick", "член",
        Map.of(1, " см",
            100, " м",
            100_000, " км",
            100_000_000, "к км"
    )),

    EMOTIONAL_INTELLIGENCE("emotional_intelligence", "эмоциональный интеллект",
        Map.of(1, " женских сил")
    );

    private final String key;
    private final String displayName;
    private final NavigableMap<Integer, String> units;

    GrowthStyle(String key, String displayName, Map<Integer, String> units) {
        this.key = key;
        this.displayName = displayName;
        this.units = new TreeMap<>(units);
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

    public static String convertValue(double value, GrowthStyle style) {
        Map.Entry<Integer, String> entry = style.getUnits().floorEntry((int) value);

        if (entry == null) {
            entry = style.getUnits().firstEntry();
        }

        return round(value / entry.getKey()) + entry.getValue();
    }
}
