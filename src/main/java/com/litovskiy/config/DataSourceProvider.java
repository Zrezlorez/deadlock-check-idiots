package com.litovskiy.config;

import com.litovskiy.util.PropsManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Properties;

public class DataSourceProvider {

    private static final HikariDataSource dataSource = init();

    private static HikariDataSource init() {
        Properties props = PropsManager.getProps();
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.user"));
        config.setPassword(props.getProperty("db.password"));

        config.setDriverClassName("org.postgresql.Driver");

        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1200000);
        config.setConnectionTimeout(5000);

        return new HikariDataSource(config);
    }

    public static DataSource get() {
        return dataSource;
    }
}