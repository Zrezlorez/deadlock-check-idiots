package com.litovskiy.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Data
@Validated
@RefreshScope
@ConfigurationProperties("admin")
public class AdminProperties {

    @NotNull
    private Set<Long> telegramAdminIds = Set.of();

    @NotNull
    private Set<Long> discordAdminIds = Set.of();
}
