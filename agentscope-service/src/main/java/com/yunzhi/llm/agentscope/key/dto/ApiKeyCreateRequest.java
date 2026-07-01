package com.yunzhi.llm.agentscope.key.dto;

import com.yunzhi.llm.agentscope.key.ApiKeyProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApiKeyCreateRequest {

    @NotBlank
    private String label;

    @NotNull
    private ApiKeyProvider provider;

    @NotBlank
    private String secret;

    private boolean enabled = true;

    private int priority = 0;
}
