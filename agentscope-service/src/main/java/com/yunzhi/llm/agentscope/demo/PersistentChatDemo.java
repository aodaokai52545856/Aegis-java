package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PersistentChatDemo {

    private static final String SESSION_ID = "user_hollis_session";

    public static void main(String[] args) {
        String apiKey = "sk-e4902ea9d4164c1fa9d88ca86b2645c8";

        Path sessionPath = Paths.get(System.getProperty("user.home"),
                ".agentscope", "examples", "sessions");
        AgentStateStore stateStore = new JsonFileAgentStateStore(sessionPath);

        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant with persistent memory. ")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-max")
                        .build())
                .stateStore(stateStore)
                .defaultSessionId(SESSION_ID)
                .build();

        boolean resumed = stateStore.exists(null, SESSION_ID);
        agent.getAgentState();
        if (resumed) {
            System.out.println("Session restored! "
                    + agent.getAgentState().getContext().size() + " messages loaded.");
        } else {
            System.out.println("New session started.");
        }

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("What's my name and what do I do?").build())
                .build();

        Msg response = agent.call(userMsg).block();
        System.out.println("Agent: " + response.getTextContent());

        agent.saveAgentState(null, SESSION_ID);
        System.out.println("Session saved. Messages in context: "
                + agent.getAgentState().getContext().size());
    }
}
