package com.litovskiy.service;

import com.litovskiy.dao.PlayerDao;
import com.litovskiy.entity.GrowthStyle;
import com.litovskiy.entity.Platform;
import com.litovskiy.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class DickService {

    private final PlayerDao playerDao;
    private final PlayerAccountService playerAccountService;
    private final ActivityService activityService;
    private final ConversationStyleService conversationStyleService;
    private final GameConfigService gameConfigService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Random random;

    public DickService(PlayerDao playerDao,
                       PlayerAccountService playerAccountService,
                       ActivityService activityService,
                       ConversationStyleService conversationStyleService,
                       GameConfigService gameConfigService) {
        this(playerDao, playerAccountService, activityService, conversationStyleService, gameConfigService, new Random());
    }

    public DickService(PlayerDao playerDao,
                       PlayerAccountService playerAccountService,
                       ActivityService activityService,
                       ConversationStyleService conversationStyleService,
                       GameConfigService gameConfigService,
                       Random random) {
        this.playerDao = playerDao;
        this.playerAccountService = playerAccountService;
        this.activityService = activityService;
        this.conversationStyleService = conversationStyleService;
        this.gameConfigService = gameConfigService;
        this.random = random;
    }

    public String grow(Platform platform, long profileId) {
        return grow(platform, profileId, null);
    }

    public String grow(Platform platform, long profileId, Long scopeId) {
        LocalDateTime now = LocalDateTime.now();
        Player player = playerAccountService.resolveOrCreate(platform, profileId);

        LocalDateTime lastTime = player.getLastGrowTime();
        if (lastTime != null) {
            int timeRange = gameConfigService.getInt(GameSetting.COOLDOWN_RANGE);
            ChronoUnit timeUnit = gameConfigService.getChronoUnit(GameSetting.COOLDOWN_UNIT);
            LocalDateTime nextAllowed = lastTime.plus(timeRange, timeUnit);
            if (now.isBefore(nextAllowed)) {
                return "Вы уже растили показатель, следующая попытка будет в " + nextAllowed.format(formatter);
            }
        }

        double oldValue = player.getSize();
        double activityBonus = activityService == null ? 1.0 : activityService.getGrowthBonusMultiplier(platform, profileId, scopeId);
        double newValue = round(oldValue * getGrowth(oldValue, activityBonus));

        player.setSize(newValue);
        player.setLastGrowTime(now);
        playerDao.save(player);

        GrowthStyle style = conversationStyleService == null
            ? GrowthStyle.DICK
            : conversationStyleService.getStyle(platform, scopeId);

        return String.format("Ваш %s вырос на %s. Текущий размер: %s",
            style.displayName(),
            convertValue(newValue - oldValue),
            convertValue(newValue)
        );
    }

    private double getGrowth(double currentSize, double activityBonus) {
        double dickGrowModifier = gameConfigService.getDouble(GameSetting.GROWTH_MEAN);
        double minGrowModifier = gameConfigService.getDouble(GameSetting.GROWTH_MIN);
        double maxGrowModifier = gameConfigService.getDouble(GameSetting.GROWTH_MAX);
        double slowScale = gameConfigService.getDouble(GameSetting.SLOW_SCALE);
        double baseGrowth = dickGrowModifier + random.nextGaussian() * 0.01;
        baseGrowth = Math.max(minGrowModifier, Math.min(maxGrowModifier, baseGrowth));
        double slowdown = 1 / (1 + currentSize / slowScale);
        return 1 + (baseGrowth - 1) * slowdown * activityBonus;
    }

    private String convertValue(double sm) {
        if (sm > 100_000_000) {
            return round(sm / 100_000_000) + "к км";
        }

        if (sm > 100_000) {
            return round(sm / 100_000) + " км";
        }

        if (sm > 100) {
            return round(sm / 100) + " м";
        }

        return round(sm) + " см";
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
