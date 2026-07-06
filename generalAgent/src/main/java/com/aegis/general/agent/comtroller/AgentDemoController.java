package com.aegis.general.agent.comtroller;

import com.aegis.general.agent.config.AegisProperties;
import com.aegis.general.agent.tools.WeatherTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "Agent", description = "AI agent demo and stream endpoints")
@RestController
@RequestMapping(path = "/api/v1")
public class AgentDemoController {

    private final ChatClient chatClient;

    private final Map<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();

    @Autowired
    private AegisProperties aegisProperties;

    public AgentDemoController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * demo1 是最简单的例子 接收一个简单string 类型消息 返回一个 string 类型消息
     * */
    @Operation(
            summary = "Chat with the demo agent",
            description = "Sends a single message to the configured DeepSeek model and returns one text response.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Agent text response",
                            content = @Content(schema = @Schema(implementation = String.class))
                    )
            }
    )
    @GetMapping(path = "/agent/chat/demo1")
    public String chatDemo(
            @Parameter(description = "Message sent to the agent", example = "What is the weather in Beijing?")
            @RequestParam String message){
        // 先创建一个模型
        OpenAiChatModel deepseekModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(aegisProperties.getDeepseek().getBaseUrl())
                        .apiKey(aegisProperties.getAdminToken())
                        .model(aegisProperties.getDeepseek().getDefaultModel())
                        .temperature(0.5)
                        .build())
                .build();
        String groqResponse = ChatClient.builder(deepseekModel)
                .build()
                .prompt(message)
                .tools(new WeatherTool())
                .call().content();
        return groqResponse;
    }

    /**
     * 流式 demo1 是最简单的例子 接收一个简单string 类型消息 返回一个 流式 类型消息
     * */
    @Operation(
            summary = "Stream agent messages",
            description = "SSE endpoint reserved for agent streaming output debugging.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Text stream payload",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "hello agent")
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Server-Sent Events stream",
                            content = @Content(
                                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                    schema = @Schema(implementation = String.class)
                            )
                    )
            }
    )
    @PostMapping(path = "/agent/stream/demo1", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody String request) {
        // 先创建一个模型
        OpenAiChatModel deepseekModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(aegisProperties.getDeepseek().getBaseUrl())
                        .apiKey(aegisProperties.getAdminToken())
                        .model(aegisProperties.getDeepseek().getDefaultModel())
                        .temperature(0.5)
                        .build())
                .build();
        Flux<String> response = ChatClient.builder(deepseekModel)
                .build()
                .prompt(request)
                .tools(new WeatherTool())
                .stream().content();
        return response;
    }

    /**
     *  带记忆的（conversationId  短期记忆 + 滑动窗口） 的 能调用工具的  demo2
     * */
    @PostMapping(path = "/agent/stream/demo2", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> memoryStream(@RequestBody String request,
                                     @RequestParam String conversationId){
        // 先创建一个模型
        OpenAiChatModel deepseekModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(aegisProperties.getDeepseek().getBaseUrl())
                        .apiKey(aegisProperties.getAdminToken())
                        .model(aegisProperties.getDeepseek().getDefaultModel())
                        .temperature(0.5)
                        .build())
                .build();

        ChatMemory chatMemory = memoryMap.computeIfAbsent(conversationId, id -> MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build());

        Flux<String> response = ChatClient.builder(deepseekModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build()
                .prompt(request)
                .tools(new WeatherTool())
                .advisors(advisorSpec ->  advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream().content();
        return response;
    }




    /**
     *  带记忆的（session） 的 能调用工具的  能调用skill 的demo
     * */

    /**
     *  带记忆的（session） 的 能调用工具的  能调用skill 的demo
     * */

    /**
     *  带记忆的（session） 的 能调用工具的  能调用skill 能调用 rag 的demo
     * */

}
