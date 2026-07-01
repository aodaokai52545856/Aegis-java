package com.yunzhi.llm.agentscope.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
public class SqliteDataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String home = System.getProperty("user.home");
        Path dbDir = Path.of(home, ".agentscope");
        try {
            Files.createDirectories(dbDir);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create SQLite directory: " + dbDir, e);
        }
        String dbPath = dbDir.resolve("aegis-keys.db").toString();
        log.info("Using SQLite database at {}", dbPath);
        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:" + dbPath)
                .build();
    }
}
