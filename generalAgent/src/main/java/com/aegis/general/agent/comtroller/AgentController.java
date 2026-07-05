package com.aegis.general.agent.comtroller;

import com.aegis.general.agent.config.AegisProperties;
import com.aegis.general.agent.tools.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api/v1")
public class AgentController {

    private final ChatClient chatClient;

    @Autowired
    private AegisProperties aegisProperties;

    public AgentController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }



    @PostMapping(path = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(Flux<String> request) {

        return request;
    }


    @GetMapping(path = "/agent/chat/demo1")
    public String chatDemo(String message){
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

}
