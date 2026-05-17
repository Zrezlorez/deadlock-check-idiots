package com.litovskiy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.util.Random;

@Configuration
public class BaseClassConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean()
    public Random random() {
        return new Random();
    }
}
