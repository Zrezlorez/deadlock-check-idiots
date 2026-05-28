package com.litovskiy.config.cloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubConfigService {

    private final ConfigurableEnvironment environment;
    private final ContextRefresher contextRefresher;

    public synchronized ReloadResult reload() {
        GithubConfigProperties props = GithubConfigProperties.from(environment);
        if (!props.enabled()) {
            return ReloadResult.disabled();
        }
        if (!props.isComplete()) {
            return ReloadResult.failure("Конфигурация cloud-config неполная (owner/repo/path/token)");
        }

        try {
            Set<String> changedKeys = contextRefresher.refresh();
            log.info("Reloaded cloud config from GitHub {} (changed keys: {})", props.describe(), changedKeys.size());
            return ReloadResult.success(props.describe(), changedKeys);
        } catch (Exception e) {
            log.warn("Failed to reload cloud config from GitHub {}: {}", props.describe(), e.getMessage());
            return ReloadResult.failure(e.getMessage());
        }
    }

    public record ReloadResult(boolean ok, String message) {
        static ReloadResult success(String source, Set<String> changed) {
            String suffix = changed.isEmpty()
                ? "без изменений"
                : "изменено " + changed.size() + " свойств: " + String.join(", ", changed);
            return new ReloadResult(true, "Конфиг перезагружен из " + source + " (" + suffix + ")");
        }

        static ReloadResult failure(String reason) {
            return new ReloadResult(false, "Не удалось перезагрузить конфиг: " + reason);
        }

        static ReloadResult disabled() {
            return new ReloadResult(false, "Cloud-config отключён (cloud-config.enabled=false)");
        }
    }
}
