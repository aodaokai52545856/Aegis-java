package com.yunzhi.llm.agentscope.key;

import com.yunzhi.llm.agentscope.config.AegisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyBootstrap {

    private final AegisProperties aegisProperties;
    private final ApiKeyRepository apiKeyRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncKeysFromConfig() {
        var configured = aegisProperties.getDeepseek().getApiKeys();
        if (configured.isEmpty()) {
            log.warn("No DeepSeek API keys in config (aegis.deepseek.api-keys). "
                    + "Set DEEPSEEK_API_KEY env or add keys to application.yml.");
            return;
        }

        int synced = 0;
        for (AegisProperties.ApiKeyItem item : configured) {
            if (item.getSecret() == null || item.getSecret().isBlank()) {
                log.warn("Skipping API key '{}' — secret is empty", item.getLabel());
                continue;
            }
            String label = item.getLabel() == null || item.getLabel().isBlank()
                    ? "default"
                    : item.getLabel();

            ApiKeyEntity entity = apiKeyRepository
                    .findByProviderAndLabel(ApiKeyProvider.DEEPSEEK, label)
                    .orElseGet(() -> {
                        ApiKeyEntity created = new ApiKeyEntity();
                        created.setLabel(label);
                        created.setProvider(ApiKeyProvider.DEEPSEEK);
                        return created;
                    });

            entity.setSecret(item.getSecret().trim());
            entity.setEnabled(item.isEnabled());
            entity.setPriority(item.getPriority());
            apiKeyRepository.save(entity);
            synced++;
            log.info("Synced DeepSeek API key '{}' (enabled={}, priority={})",
                    label, entity.isEnabled(), entity.getPriority());
        }

        if (synced == 0) {
            log.warn("No valid DeepSeek API keys synced — check aegis.deepseek.api-keys configuration");
        }
    }
}
