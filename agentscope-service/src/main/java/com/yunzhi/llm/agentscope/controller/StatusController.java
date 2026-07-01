package com.yunzhi.llm.agentscope.controller;

import com.yunzhi.llm.agentscope.key.ApiKeyProvider;
import com.yunzhi.llm.agentscope.key.ApiKeyRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StatusController {

    private final ApiKeyRepository apiKeyRepository;

    @GetMapping("/status")
    public StatusResponse status() {
        boolean keysConfigured = !apiKeyRepository
                .findByProviderAndEnabledTrueOrderByPriorityDescIdAsc(ApiKeyProvider.DEEPSEEK)
                .isEmpty();
        return StatusResponse.builder()
                .ready(keysConfigured)
                .keysConfigured(keysConfigured)
                .build();
    }

    @Data
    @Builder
    public static class StatusResponse {
        private boolean ready;
        private boolean keysConfigured;
    }
}
