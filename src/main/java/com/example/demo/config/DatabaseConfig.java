package com.example.demo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    // --- 1. PRIMARY DATASOURCE (Full R/W Permissions) ---
    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource primaryDataSource) {
        return new JdbcTemplate(primaryDataSource);
    }

    // --- 2. SECONDARY DATASOURCE (Read-Only AI Executions) ---
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.readonly")
    public DataSource readOnlyDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate readOnlyJdbcTemplate(@Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        return new JdbcTemplate(readOnlyDataSource);
    }
}