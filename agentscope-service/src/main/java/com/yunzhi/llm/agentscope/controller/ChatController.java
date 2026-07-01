package com.yunzhi.llm.agentscope.controller;

import com.yunzhi.llm.agentscope.chat.AgentChatService;
import com.yunzhi.llm.agentscope.chat.PlainChatService;
import com.yunzhi.llm.agentscope.chat.dto.ChatStreamRequest;
import com.yunzhi.llm.agentscope.chat.dto.ModelInfoResponse;
import com.yunzhi.llm.agentscope.config.AegisProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final PlainChatService plainChatService;
    private final AgentChatService agentChatService;
    private final AegisProperties aegisProperties;

    @GetMapping("/models")
    public List<ModelInfoResponse> models() {
        return aegisProperties.getDeepseek().getModels().stream()
                .map(item -> ModelInfoResponse.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .build())
                .toList();
    }

    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @Valid @RequestBody ChatStreamRequest request,
            HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return plainChatService.stream(request);
    }

    @PostMapping(path = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentStream(
            @Valid @RequestBody ChatStreamRequest request,
            HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return agentChatService.stream(request);
    }
}
