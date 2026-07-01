package com.yunzhi.llm.agentscope.controller;

import com.yunzhi.llm.agentscope.chat.dto.ChatStreamRequest;
import com.yunzhi.llm.agentscope.chat.PlainChatService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @deprecated Use {@link ChatController} instead.
 */
@Deprecated
@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamingController {

    private final PlainChatService plainChatService;

    @GetMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam String message,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String sessionId,
            HttpServletResponse httpServletResponse) {
        httpServletResponse.setCharacterEncoding("UTF-8");

        ChatStreamRequest request = new ChatStreamRequest();
        request.setMessage(message);
        request.setModel(model);
        request.setSessionId(sessionId);
        return plainChatService.stream(request);
    }
}
