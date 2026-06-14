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
@ConfigurationProperties("game.growth")
public class GrowthProperties {

    @NotNull
    private Duration growthCooldown;

    @Min(0)
    @NotNull
    private Double startSize;

    @Min(1)
    @NotNull
    private Double growthMean;

    @Min(1)
    @NotNull
    private Double growthMin;

    @Min(1)
    @NotNull
    private Double growthMax;

    @Min(0)
    @NotNull
    private Double growthLimit;

    @Min(0)
    @NotNull
    private Double growthGauss;

    @NotNull
    @Range(min = 0, max = 1)
    private Double failChance;

    @NotNull
    @Range(min = 0, max = 1)
    private Double failPercent;

    @NotNull
    @Range(min = 0, max = 1)
    private Double offlineFailChance;

    @NotNull
    @Range(min = 0, max = 1)
    private Double offlineFailPercent;

    @NotNull
    @Range(min = 0, max = 1)
    private Double critChance;

    @Min(0)
    @NotNull
    private Double critMultiplier;
}
