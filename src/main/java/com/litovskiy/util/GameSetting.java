package com.litovskiy.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;

@RequiredArgsConstructor
@Getter
public enum GameSetting {
    // настройки роста
    START_SIZE(
        SettingGroup.GROWTH,
        "start_size",
        "1.0",
        "Стартовый размер нового игрока"
    ),

    COOLDOWN_UNIT(
        SettingGroup.GROWTH,
        "cooldown_unit",
        "HOURS",
        "Единица кулдауна: SECONDS, MINUTES, HOURS"
    ),

    COOLDOWN_RANGE(
        SettingGroup.GROWTH,
        "cooldown_range",
        "1",
        "Длина кулдауна в выбранной единице"),

    GROWTH_MEAN(
        SettingGroup.GROWTH,
        "growth_mean",
        "1.10",
        "Базовый коэффициент роста"),

    GROWTH_MIN(
        SettingGroup.GROWTH,
        "growth_min",
        "1.02",
        "Минимальный коэффициент роста"),

    GROWTH_MAX(
        SettingGroup.GROWTH,
        "growth_max",
        "1.20",
        "Максимальный коэффициент роста"),

    SLOW_SCALE(
        SettingGroup.GROWTH,
        "slow_scale",
        "100000.0",
        "Масштаб замедления роста"),

    FAIL_CHANCE(
        SettingGroup.GROWTH,
        "fail_chance",
        "0.10",
        "Шанс неудачи при росте от 0 до 1"),

    FAIL_PERCENT(
        SettingGroup.GROWTH,
        "fail_percent",
        "0.10",
        "Процент уменьшения при неудаче от 0 до 1"),

    CRIT_CHANCE(
        SettingGroup.GROWTH,
        "crit_chance",
        "0.15",
        "Шанс критического успеха при росте от 0 до 1"),

    CRIT_MULTIPLIER(
        SettingGroup.GROWTH,
        "crit_multiplier",
        "1.5",
        "Множитель прироста при джекпоте"),

    // настройки способностей
    ABILITY_COOLDOWN_UNIT(
        SettingGroup.ABILITIES,
        "ability_cooldown_unit",
        "HOURS",
        "Единица кулдауна способности: SECONDS, MINUTES, HOURS"),

    ABILITY_COOLDOWN_RANGE(
        SettingGroup.ABILITIES,
        "ability_cooldown_range",
        "8",
        "Длина кулдауна способности в выбранной единице"),

    ABILITY_TRANSFER_COMMISSION(
        SettingGroup.ABILITIES,
        "ability_transfer_commission",
        "0.15",
        "Комиссия перевода"),

    // способности на врага
    FUCK_COST_PERCENT(
        SettingGroup.ENEMY_ABILITIES,
        "fuck_cost_percent",
        "0.04",
        "Цена за луз для цели"),

    FUCK_FAIL_CHANCE_PENALTY(
        SettingGroup.ENEMY_ABILITIES,
        "fuck_fail_chance_penalty",
        "0.18",
        "Доп. шанс неудачи для цели"),

    SLOW_GROWTH_PENALTY(
        SettingGroup.ENEMY_ABILITIES,
        "slow_growth_penalty",
        "-0.25",
        "Уменьшение роста для цели"),

    // способности на себя
    JACKPOT_FAIL_CHANCE(
        SettingGroup.SELF_ABILITIES,
        "jackpot_fail_chance",
        "0.15",
        "Доп. шанс неудачи при джекпоте"),

    JACKPOT_CRIT_CHANCE(
        SettingGroup.SELF_ABILITIES,
        "jackpot_crit_chance",
        "0.25",
        "Доп. шанс удачи при джекпоте"),

    PRAY_FAIL_BONUS(
        SettingGroup.SELF_ABILITIES,
        "pray_fail_bonus",
        "0.10",
        "Уменьшение шанса неудачи для себя"),

    TURTLE_GROWTH_BONUS(
        SettingGroup.SELF_ABILITIES,
        "turtle_growth_bonus",
        "0.25",
        "Уменьшение роста для цели"),

    // MAX
    MAX_PENDING_FAIL_CHANCE(
        SettingGroup.LIMITS,
        "max_pending_fail_chance",
        "0.5",
        "Максимальный шанс неудачи"),

    MAX_PENDING_CRIT_CHANCE(
        SettingGroup.LIMITS,
        "max_pending_crit_chance",
        "0.75",
        "Максимальный шанс удачи"),

    MIN_PENDING_GROWTH(
        SettingGroup.LIMITS,
        "min_pending_growth",
        "-0.95",
        "Максимальный штраф роста"),

    MAX_PENDING_GROWTH(
        SettingGroup.LIMITS,
        "max_pending_growth",
        "5",
        "Максимальный бонус роста"),

    // OTHER
    ACTIVITY_LOOKBACK_DAYS(
        SettingGroup.OTHER,
        "activity_lookback_days",
        "7",
        "Окно активности в днях"),

    ACTIVITY_MAX_GROWTH_BONUS(
        SettingGroup.OTHER,
        "activity_max_growth_bonus",
        "0.15",
        "Максимальный бонус активности на платформу"),

    LEADERBOARD_LIMIT(
        SettingGroup.OTHER,
        "leaderboard_limit",
        "5",
        "Количество строк в лидерборде"),

    LOG_PAGE_SIZE(
        SettingGroup.OTHER,
        "log_page_size",
        "5",
        "Размер строки логов"
    );



    private final SettingGroup group;
    private final String key;
    private final String defaultValue;
    private final String description;


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
            case FAIL_CHANCE, FAIL_PERCENT, CRIT_CHANCE, FUCK_COST_PERCENT, FUCK_FAIL_CHANCE_PENALTY,
                 JACKPOT_FAIL_CHANCE, JACKPOT_CRIT_CHANCE, SLOW_GROWTH_PENALTY, TURTLE_GROWTH_BONUS,
                 MAX_PENDING_FAIL_CHANCE, MAX_PENDING_CRIT_CHANCE,
                 MIN_PENDING_GROWTH, MAX_PENDING_GROWTH, PRAY_FAIL_BONUS,
                 ABILITY_TRANSFER_COMMISSION -> {
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
            case COOLDOWN_RANGE, ABILITY_COOLDOWN_RANGE, ACTIVITY_LOOKBACK_DAYS, LEADERBOARD_LIMIT, LOG_PAGE_SIZE -> {
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
