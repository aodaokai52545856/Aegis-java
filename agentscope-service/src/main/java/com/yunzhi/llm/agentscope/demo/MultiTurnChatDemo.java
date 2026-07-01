package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;

public class MultiTurnChatDemo {

    public static void main(String[] args) {
        String apiKey = "sk-e4902ea9d4164c1fa9d88ca86b2645c8";

        // 2.0：对话历史由 AgentState.context 维护，无需单独注入 InMemoryMemory
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant. Remember what the user tells you.")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-max")
                        .build())
                .build();

        Msg msg1 = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("My name is Hollis and I'm a software engineer.").build())
                .build();
        Msg reply1 = agent.call(msg1).block();
        System.out.println("Agent: " + reply1.getTextContent());

        Msg msg2 = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("What's my name and what do I do?").build())
                .build();
        Msg reply2 = agent.call(msg2).block();
        System.out.println("Agent: " + reply2.getTextContent());

        System.out.println("Total messages in context: "
                + agent.getAgentState().getContext().size());
    }
}
