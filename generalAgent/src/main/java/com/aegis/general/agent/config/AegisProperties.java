package com.aegis.general.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "aegis")
public class AegisProperties {

    private String adminToken = "change-me-in-production";
    private DeepSeekProperties deepseek = new DeepSeekProperties();
    private CorsProperties cors = new CorsProperties();

    @Data
    public static class DeepSeekProperties {
        private String baseUrl = "https://api.deepseek.com";
        private String defaultModel = "deepseek-chat";
        private List<ModelItem> models = new ArrayList<>();
        /** Server-managed keys, synced to DB on startup. */
        private List<ApiKeyItem> apiKeys = new ArrayList<>();
    }

    @Data
    public static class ApiKeyItem {
        private String label = "default";
        private String secret = "";
        private boolean enabled = true;
        private int priority = 10;
    }

    @Data
    public static class ModelItem {
        private String id;
        private String name;
    }

    @Data
    public static class CorsProperties {
        private List<String> allowedOrigins = new ArrayList<>();
    }
}
