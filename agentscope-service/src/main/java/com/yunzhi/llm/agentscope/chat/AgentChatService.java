package com.yunzhi.llm.agentscope.chat;

import com.yunzhi.llm.agentscope.chat.dto.ChatStreamRequest;
import com.yunzhi.llm.agentscope.key.AllocatedKey;
import com.yunzhi.llm.agentscope.key.ApiKeyPoolService;
import com.yunzhi.llm.agentscope.key.ApiKeyProvider;
import com.yunzhi.llm.agentscope.model.ModelFactory;
import com.yunzhi.llm.agentscope.tools.SimpleTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AgentChatService {

    private static final String INTERNAL_FRAGMENT_TOOL = "__fragment__";

    private final ApiKeyPoolService apiKeyPoolService;
    private final ModelFactory modelFactory;
    private final ConcurrentHashMap<String, ReActAgent> sessionAgents = new ConcurrentHashMap<>();

    public Flux<String> stream(ChatStreamRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        String modelId = request.getModel();
        AllocatedKey allocated = apiKeyPoolService.allocate(
                ApiKeyProvider.DEEPSEEK, sessionId, modelId, "agent");

        ReActAgent agent = sessionAgents.computeIfAbsent(sessionId, id -> buildAgent(allocated, modelId));

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(request.getMessage()).build())
                .build();

        StreamOptions streamOptions = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT, EventType.AGENT_RESULT, EventType.SUMMARY)
                .incremental(true)
                .includeReasoningChunk(true)
                .includeReasoningResult(false)
                .includeActingChunk(false)
                .includeSummaryChunk(true)
                .includeSummaryResult(true)
                .build();

        return agent.stream(userMsg, streamOptions)
                .subscribeOn(Schedulers.boundedElastic())
                .concatMap(this::mapEventToFlux)
                .concatWith(Flux.just(SseEvents.done(allocated.requestId(), allocated.keyId())))
                .onErrorResume(ex -> Flux.just(
                        SseEvents.error(ex.getMessage()),
                        SseEvents.done(allocated.requestId(), allocated.keyId())));
    }

    private ReActAgent buildAgent(AllocatedKey allocated, String modelId) {
        Model model = modelFactory.createDeepSeekModel(allocated, modelId);
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new SimpleTools());

        return ReActAgent.builder()
                .name("TauriAgent")
                .sysPrompt("你是中文助手。需要实时信息时调用工具（含 MCP 工具）。"
                        + "工具执行完成后，用一段简洁的中文给出最终总结答复，不要重复工具已返回的原始数据。")
                .model(model)
                .toolkit(toolkit)
                .build();
    }

    private Flux<String> mapEventToFlux(Event event) {
        if (event == null || event.getMessage() == null) {
            return Flux.empty();
        }

        Msg msg = event.getMessage();
        EventType type = event.getType();

        if (type == EventType.AGENT_RESULT || type == EventType.SUMMARY) {
            String text = msg.getTextContent();
            if (text != null && !text.isBlank()) {
                return Flux.just(SseEvents.textDelta(text));
            }
            return Flux.empty();
        }

        if (type == EventType.REASONING) {
            Flux<String> flux = Flux.empty();
            String text = msg.getTextContent();
            if (text != null && !text.isBlank()) {
                flux = flux.concatWith(Flux.just(SseEvents.thinkingDelta(text)));
            }
            flux = flux.concatWith(Flux.fromIterable(msg.getContentBlocks(ToolUseBlock.class))
                    .filter(toolUse -> isUserVisibleTool(toolUse.getName()))
                    .map(toolUse -> SseEvents.toolStart(toolUse.getName(), toolUse.getInput())));
            return flux;
        }

        if (type == EventType.TOOL_RESULT) {
            return Flux.fromIterable(msg.getContentBlocks(ToolResultBlock.class))
                    .filter(result -> isUserVisibleTool(result.getName()))
                    .map(result -> SseEvents.toolResult(result.getName(), formatToolOutput(result)));
        }

        return Flux.empty();
    }

    private boolean isUserVisibleTool(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return !INTERNAL_FRAGMENT_TOOL.equals(name);
    }

    private String formatToolOutput(ToolResultBlock result) {
        if (result.getOutput() == null || result.getOutput().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (var block : result.getOutput()) {
            if (block instanceof TextBlock textBlock) {
                sb.append(textBlock.getText());
            } else {
                sb.append(block.toString());
            }
        }
        return sb.toString();
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }
}
