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
@ConfigurationProperties("game")
public class GameProperties {

    @Min(1)
    @NotNull
    private Integer leaderboardLimit;

    @Min(1)
    @NotNull
    private Integer logPageSize;
}
