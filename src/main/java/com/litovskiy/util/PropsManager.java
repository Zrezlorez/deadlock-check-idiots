package com.litovskiy.util;

import com.litovskiy.config.DataSourceProvider;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropsManager {

    @Getter
    private static final Properties props = init();

    private PropsManager() {
    }

    private static Properties init() {
        Properties props = new Properties();
        try (InputStream input = DataSourceProvider.class
            .getClassLoader()
            .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties not found");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return props;
    }
}
