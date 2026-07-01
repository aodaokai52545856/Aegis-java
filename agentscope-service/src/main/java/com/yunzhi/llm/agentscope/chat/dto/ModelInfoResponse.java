package com.yunzhi.llm.agentscope.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelInfoResponse {

    private String id;
    private String name;
}
