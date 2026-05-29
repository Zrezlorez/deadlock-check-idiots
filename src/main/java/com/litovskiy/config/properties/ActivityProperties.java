package com.litovskiy.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@RefreshScope
@ConfigurationProperties("game.activity")
public class ActivityProperties {

    @Min(0)
    @NotNull
    private Integer lookBackDays;

    @Min(0)
    @NotNull
    private Double maxGrowthBonus;
}
