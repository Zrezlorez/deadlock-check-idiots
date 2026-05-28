package com.litovskiy.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@RefreshScope
@ConfigurationProperties("game.ability")
public class AbilityProperties {

    @NotNull
    private Duration abilityCooldown;

    @NotNull
    @Range(min = 0, max = 1)
    private Double jackpotFailChance;

    @NotNull
    @Range(min = 0, max = 1)
    private Double jackpotCritChance;

    @NotNull
    @Range(min = 0, max = 1)
    private Double prayFailBonus;

    @Min(0)
    @NotNull
    private Double turtleGrowthBonus;

    @NotNull
    @Range(min = 0, max = 1)
    private Double fuckCostPercent;
    @NotNull
    @Range(min = 0, max = 1)
    private Double fuckFailChancePenalty;

    @NotNull
    @Range(min = -1, max = 0)
    private Double slowGrowthPenalty;

    @NotNull
    @Range(min = 0, max = 1)
    private Double transferCommission;

    @NotNull
    @Range(min = 0, max = 1)
    private Double maxPendingFailChance;

    @NotNull
    @Range(min = 0, max = 1)
    private Double maxPendingCritChance;

    @NotNull
    @Range(min = -1, max = 0)
    private Double minPendingGrowth;

    @Min(0)
    @NotNull
    private Double maxPendingGrowth;
}
