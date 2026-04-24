package com.litovskiy.service;

import com.litovskiy.dao.GenericDao;
import com.litovskiy.entity.Player;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class DickService {

    private final GenericDao<Player> playerRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Random random = new Random();

    private ChronoUnit timeUnit = ChronoUnit.SECONDS;
    private int timeRange = 1;

    private double startDickSize = 1.0;
    private double dickGrowModifier = 1.15;
    private double minGrowModifier = 1.02;
    private double maxGrowModifier = 1.1;
    private double slowScale = 100_000_000.0;

    public DickService(GenericDao<Player> playerRepository) {
        this.playerRepository = playerRepository;
    }

    public String grow(long chatId) {
        LocalDateTime now = LocalDateTime.now();

        Player player = playerRepository.find(chatId);
        if (player == null) {
            player = new Player(chatId, startDickSize);
        }

        LocalDateTime lastTime = player.getLastGrowTime();
        LocalDateTime nextAllowed = lastTime.plus(timeRange, timeUnit);

        if (now.isBefore(nextAllowed)) {
            return "Вы уже растили член, следующая попытка будет в "
                + nextAllowed.format(formatter);
        }

        double oldDickSize = player.getSize();
        double newDickSize = round(oldDickSize * getGrowth(oldDickSize));

        player.setSize(newDickSize);
        player.setLastGrowTime(now);
        playerRepository.save(player);

        return String.format("Ваш член вырос на %s. Текущий размер: %s",
            convertValue(newDickSize - oldDickSize),
            convertValue(newDickSize)
        );
    }

    private double getGrowth(double currentSize) {
        double baseGrowth = dickGrowModifier + random.nextGaussian() * 0.01;
        baseGrowth = Math.max(minGrowModifier, Math.min(maxGrowModifier, baseGrowth));
        double slowdown = 1 / (1 + currentSize / slowScale);
        return 1 + (baseGrowth - 1) * slowdown;
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
