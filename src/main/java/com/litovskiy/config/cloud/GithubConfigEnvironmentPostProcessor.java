package com.litovskiy.config.cloud;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

public class GithubConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final DeferredLog log = new DeferredLog();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        GithubConfigProperties props = GithubConfigProperties.from(environment);
        if (!props.enabled()) {
            return;
        }

        if (!props.isComplete()) {
            log.warn("Cloud config is enabled but configuration is incomplete (owner/repo/path/token must be set)");
            registerDeferredLog(application);
            return;
        }

        try {
            Map<String, Object> data = GithubConfigLoader.load(props);
            environment.getPropertySources().addFirst(new GithubConfigPropertySource(data));
            log.info("Loaded cloud config from GitHub " + props.describe() + " (" + data.size() + " properties)");
        } catch (Exception e) {
            log.warn("Failed to load cloud config from GitHub " + props.describe() + ": " + e.getMessage());
        }

        registerDeferredLog(application);
    }

    private void registerDeferredLog(SpringApplication application) {
        application.addInitializers(context -> log.switchTo(GithubConfigEnvironmentPostProcessor.class));
    }
}
