package com.yunzhi.llm.agentscope.demo;

import com.yunzhi.llm.agentscope.middleware.LoggingMiddleware;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;

public class HookChatDemo {

    public static void main(String[] args) {
        String apiKey = "sk-8996fd8e3d6f42359b0cd2d8cd3a656b";

        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant. Remember what the user tells you.")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-max")
                        .build())
                .middleware(new LoggingMiddleware())
                .build();

        Msg message = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("My name is Hollis and I'm a software engineer.").build())
                .build()).block();

        System.out.println(message.getTextContent());
    }
}
