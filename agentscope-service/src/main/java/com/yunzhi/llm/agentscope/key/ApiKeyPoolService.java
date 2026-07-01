package com.yunzhi.llm.agentscope.key;

import com.yunzhi.llm.agentscope.config.AegisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyPoolService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageRepository apiKeyUsageRepository;
    private final AegisProperties aegisProperties;
    private final ConcurrentHashMap<ApiKeyProvider, AtomicInteger> roundRobin = new ConcurrentHashMap<>();

    @Transactional
    public AllocatedKey allocate(ApiKeyProvider provider, String sessionId, String model, String mode) {
        List<ApiKeyEntity> keys = apiKeyRepository.findByProviderAndEnabledTrueOrderByPriorityDescIdAsc(provider);
        if (!keys.isEmpty()) {
            return allocateFromDatabase(keys, provider, sessionId, model, mode);
        }

        List<AegisProperties.ApiKeyItem> configKeys = resolveConfigKeys(provider);
        if (!configKeys.isEmpty()) {
            log.debug("Using configured API key for {} (database pool empty)", provider);
            return allocateFromConfig(configKeys, provider, sessionId, model, mode);
        }

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No enabled API keys for provider: " + provider
                        + ". Configure aegis.deepseek.api-keys or set DEEPSEEK_API_KEY.");
    }

    private AllocatedKey allocateFromDatabase(
            List<ApiKeyEntity> keys, ApiKeyProvider provider, String sessionId, String model, String mode) {
        AtomicInteger counter = roundRobin.computeIfAbsent(provider, p -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), keys.size());
        ApiKeyEntity selected = keys.get(index);

        selected.setTotalRequests(selected.getTotalRequests() + 1);
        selected.setLastUsedAt(Instant.now());
        apiKeyRepository.save(selected);

        String requestId = UUID.randomUUID().toString();
        recordUsage(selected.getId(), requestId, sessionId, model, mode);
        return new AllocatedKey(selected.getId(), selected.getSecret(), requestId);
    }

    private AllocatedKey allocateFromConfig(
            List<AegisProperties.ApiKeyItem> configKeys,
            ApiKeyProvider provider,
            String sessionId,
            String model,
            String mode) {
        AtomicInteger counter = roundRobin.computeIfAbsent(provider, p -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), configKeys.size());
        AegisProperties.ApiKeyItem selected = configKeys.get(index);

        String requestId = UUID.randomUUID().toString();
        return new AllocatedKey(null, selected.getSecret().trim(), requestId);
    }

    private List<AegisProperties.ApiKeyItem> resolveConfigKeys(ApiKeyProvider provider) {
        if (provider != ApiKeyProvider.DEEPSEEK) {
            return List.of();
        }
        List<AegisProperties.ApiKeyItem> resolved = new ArrayList<>();
        for (AegisProperties.ApiKeyItem item : aegisProperties.getDeepseek().getApiKeys()) {
            if (item.isEnabled() && item.getSecret() != null && !item.getSecret().isBlank()) {
                resolved.add(item);
            }
        }
        resolved.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return resolved;
    }

    private void recordUsage(Long keyId, String requestId, String sessionId, String model, String mode) {
        ApiKeyUsageEntity usage = new ApiKeyUsageEntity();
        usage.setApiKeyId(keyId);
        usage.setRequestId(requestId);
        usage.setSessionId(sessionId);
        usage.setModel(model);
        usage.setMode(mode);
        apiKeyUsageRepository.save(usage);
    }
}
