package com.litovskiy.service;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

public enum GameSetting {
    START_SIZE("start_size", "1.0", "Стартовый размер нового игрока"),
    COOLDOWN_UNIT("cooldown_unit", "HOURS", "Единица кулдауна: SECONDS, MINUTES, HOURS"),
    COOLDOWN_RANGE("cooldown_range", "1", "Длина кулдауна в выбранной единице"),
    GROWTH_MEAN("growth_mean", "1.10", "Базовый коэффициент роста"),
    GROWTH_MIN("growth_min", "1.02", "Минимальный коэффициент роста"),
    GROWTH_MAX("growth_max", "1.20", "Максимальный коэффициент роста"),
    SLOW_SCALE("slow_scale", "100000.0", "Масштаб замедления роста"),
    FAIL_CHANCE("fail_chance", "0.10", "Шанс неудачи при росте от 0 до 1"),
    FAIL_PERCENT("fail_percent", "0.10", "Процент уменьшения при неудаче от 0 до 1"),
    CRIT_CHANCE("crit_chance", "0.15", "Шанс критического успеха при росте от 0 до 1"),
    CRIT_MULTIPLIER("crit_multiplier", "1.5", "Множитель прироста при джекпоте"),
    ABILITY_COOLDOWN_UNIT("ability_cooldown_unit", "HOURS", "Единица кулдауна способности: SECONDS, MINUTES, HOURS"),
    ABILITY_COOLDOWN_RANGE("ability_cooldown_range", "8", "Длина кулдауна способности в выбранной единице"),
    ENEMY_FAIL_COST_PERCENT("enemy_fail_cost_percent", "0.04", "Цена за луз для цели"),
    ENEMY_FAIL_CHANCE_BONUS("enemy_fail_chance_bonus", "0.18", "Доп шанс луза для цели"),
    SELF_CRIT_COST_PERCENT("self_crit_cost_percent", "0.02", "Цена джекпота для себя"),
    SELF_CRIT_CHANCE_BONUS("self_crit_chance_bonus", "0.25", "Доп шанс джекпота"),
    ENEMY_GROWTH_PENALTY("enemy_growth_penalty", "0.25", "Уменьшение роста для цели"),
    ACTIVITY_LOOKBACK_DAYS("activity_lookback_days", "7", "Окно активности в днях"),
    ACTIVITY_MAX_GROWTH_BONUS("activity_max_growth_bonus", "0.15", "Максимальный бонус активности на платформу"),
    LEADERBOARD_LIMIT("leaderboard_limit", "5", "Количество строк в лидерборде"),
    LINK_CODE_LENGTH("link_code_length", "6", "Длина кода линковки"),
    LINK_CODE_LIFETIME_MINUTES("link_code_lifetime_minutes", "10", "Срок жизни кода линковки в минутах");

    private final String key;
    private final String defaultValue;
    private final String description;

    GameSetting(String key, String defaultValue, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String key() {
        return key;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String description() {
        return description;
    }

    public String normalize(String rawValue) {
        String trimmed = rawValue == null ? "" : rawValue.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Значение должно быть больше 0.");
        }

        return switch (this) {
            case START_SIZE, GROWTH_MEAN, GROWTH_MIN, GROWTH_MAX, SLOW_SCALE, ACTIVITY_MAX_GROWTH_BONUS -> {
                double value = Double.parseDouble(trimmed);
                if (value <= 0) {
                    throw new IllegalArgumentException("Значение должно быть больше 0.");
                }
                yield Double.toString(value);
            }
            case FAIL_CHANCE, FAIL_PERCENT, CRIT_CHANCE, ENEMY_FAIL_COST_PERCENT, ENEMY_FAIL_CHANCE_BONUS,
                SELF_CRIT_COST_PERCENT, SELF_CRIT_CHANCE_BONUS, ENEMY_GROWTH_PENALTY -> {
                double value = Double.parseDouble(trimmed);
                if (value < 0 || value > 1) {
                    throw new IllegalArgumentException("Значение должно быть в диапазоне от 0 до 1.");
                }
                yield Double.toString(value);
            }
            case CRIT_MULTIPLIER -> {
                double value = Double.parseDouble(trimmed);
                if (value < 1) {
                    throw new IllegalArgumentException("Значение должно быть не меньше 1.");
                }
                yield Double.toString(value);
            }
            case COOLDOWN_RANGE, ABILITY_COOLDOWN_RANGE, ACTIVITY_LOOKBACK_DAYS, LEADERBOARD_LIMIT, LINK_CODE_LENGTH,
                LINK_CODE_LIFETIME_MINUTES -> {
                int value = Integer.parseInt(trimmed);
                if (value <= 0) {
                    throw new IllegalArgumentException("Значение должно быть больше 0.");
                }
                yield Integer.toString(value);
            }
            case COOLDOWN_UNIT, ABILITY_COOLDOWN_UNIT -> ChronoUnit.valueOf(trimmed.toUpperCase(Locale.ROOT)).name();
        };
    }

    public static GameSetting fromKey(String rawKey) {
        if (rawKey == null) {
            return null;
        }

        String normalized = rawKey.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
            .filter(setting -> setting.key.equals(normalized))
            .findFirst()
            .orElse(null);
    }
}
