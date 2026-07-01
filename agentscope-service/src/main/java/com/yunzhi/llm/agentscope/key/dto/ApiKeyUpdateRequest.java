package com.yunzhi.llm.agentscope.key.dto;

import lombok.Data;

@Data
public class ApiKeyUpdateRequest {

    private String label;
    private String secret;
    private Boolean enabled;
    private Integer priority;
}
