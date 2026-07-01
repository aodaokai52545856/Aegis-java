package com.yunzhi.llm.agentscope.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatStreamRequest {

    @NotBlank
    private String message;

    private String model;

    private String sessionId;
}
