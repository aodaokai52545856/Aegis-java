package com.yunzhi.llm.agentscope.key.dto;

import com.yunzhi.llm.agentscope.key.ApiKeyEntity;
import com.yunzhi.llm.agentscope.key.ApiKeyProvider;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ApiKeyResponse {

    private Long id;
    private String label;
    private ApiKeyProvider provider;
    private boolean enabled;
    private int priority;
    private long totalRequests;
    private Instant lastUsedAt;
    private Instant createdAt;
    private String secretMasked;

    public static ApiKeyResponse from(ApiKeyEntity entity) {
        return ApiKeyResponse.builder()
                .id(entity.getId())
                .label(entity.getLabel())
                .provider(entity.getProvider())
                .enabled(entity.isEnabled())
                .priority(entity.getPriority())
                .totalRequests(entity.getTotalRequests())
                .lastUsedAt(entity.getLastUsedAt())
                .createdAt(entity.getCreatedAt())
                .secretMasked(maskSecret(entity.getSecret()))
                .build();
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
