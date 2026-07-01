package com.yunzhi.llm.agentscope.chat;

import com.yunzhi.llm.agentscope.chat.dto.ChatStreamRequest;
import com.yunzhi.llm.agentscope.key.AllocatedKey;
import com.yunzhi.llm.agentscope.key.ApiKeyPoolService;
import com.yunzhi.llm.agentscope.key.ApiKeyProvider;
import com.yunzhi.llm.agentscope.model.ModelFactory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PlainChatService {

    private final ApiKeyPoolService apiKeyPoolService;
    private final ModelFactory modelFactory;
    private final ConcurrentHashMap<String, List<Msg>> sessionHistory = new ConcurrentHashMap<>();

    public Flux<String> stream(ChatStreamRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        String modelId = request.getModel();
        AllocatedKey allocated = apiKeyPoolService.allocate(
                ApiKeyProvider.DEEPSEEK, sessionId, modelId, "chat");

        Model model = modelFactory.createDeepSeekModel(allocated, modelId);
        List<Msg> history = sessionHistory.computeIfAbsent(sessionId, id -> new ArrayList<>());

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(request.getMessage()).build())
                .build();
        history.add(userMsg);

        List<Msg> messages = List.copyOf(history);
        StringBuilder assistantText = new StringBuilder();

        return model.stream(messages, List.of(), GenerateOptions.builder().build())
                .subscribeOn(Schedulers.boundedElastic())
                .mapNotNull(this::extractTextDelta)
                .filter(delta -> !delta.isEmpty())
                .map(SseEvents::textDelta)
                .doOnNext(delta -> assistantText.append(extractContent(delta)))
                .concatWith(Flux.defer(() -> {
                    if (!assistantText.isEmpty()) {
                        history.add(Msg.builder()
                                .role(MsgRole.ASSISTANT)
                                .content(TextBlock.builder().text(assistantText.toString()).build())
                                .build());
                    }
                    return Flux.just(SseEvents.done(allocated.requestId(), allocated.keyId()));
                }))
                .onErrorResume(ex -> Flux.just(
                        SseEvents.error(ex.getMessage()),
                        SseEvents.done(allocated.requestId(), allocated.keyId())));
    }

    private String extractTextDelta(ChatResponse response) {
        if (response.getContent() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        response.getContent().forEach(block -> {
            if (block instanceof TextBlock textBlock) {
                sb.append(textBlock.getText());
            }
        });
        return sb.toString();
    }

    private String extractContent(String sseJson) {
        try {
            var obj = com.alibaba.fastjson2.JSON.parseObject(sseJson);
            return obj.getString("content");
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }
}
