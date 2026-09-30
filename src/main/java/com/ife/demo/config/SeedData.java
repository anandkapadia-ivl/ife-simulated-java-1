package com.ife.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public SeedData(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS TEST_DATA (ID INT PRIMARY KEY, MESSAGE VARCHAR(255))");
        jdbcTemplate.update("MERGE INTO TEST_DATA KEY(ID) VALUES (1, 'IFE correlated incident test data')");
    }
}
