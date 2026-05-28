package com.litovskiy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Configuration
public class BaseClassConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public DateTimeFormatter formatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
}
