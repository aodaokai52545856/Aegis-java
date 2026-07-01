package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.bailian.BailianLongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;

public class BailianAgentControlLongTermMemoryDemo {

    public static void main(String[] args) {
        BailianLongTermMemory longTermMemory = BailianLongTermMemory.builder()
                .userId("hollis789")
                .apiKey("sk-dcebc45c03b04c6e85391abb2264e594")
                .memoryLibraryId("8553b56bbeb9451295e49a09d8c26ee3")
                .build();

        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a personal assistant.")
                .model(DashScopeChatModel.builder()
                        .apiKey("sk-dcebc45c03b04c6e85391abb2264e594")
                        .modelName("qwen-max")
                        .stream(true)
                        .build())
                .longTermMemory(longTermMemory)
                .longTermMemoryMode(LongTermMemoryMode.AGENT_CONTROL)
                .build();

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("我是Hollis，我最近又不喜欢看斗破苍穹").build())
                .build();
        agent.call(userMsg).block();

        Msg userMsg2 = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("给我推荐几本小说吧").build())
                .build();

        BailianLongTermMemory longTermMemory2 = BailianLongTermMemory.builder()
                .userId("hollis789")
                .apiKey("sk-dcebc45c03b04c6e85391abb2264e594")
                .memoryLibraryId("8553b56bbeb9451295e49a09d8c26ee3")
                .build();

        ReActAgent agent2 = ReActAgent.builder()
                .name("Assistant")
                .model(DashScopeChatModel.builder()
                        .apiKey("sk-dcebc45c03b04c6e85391abb2264e594")
                        .modelName("qwen-plus")
                        .build())
                .longTermMemory(longTermMemory2)
                .longTermMemoryMode(LongTermMemoryMode.AGENT_CONTROL)
                .build();

        Msg reply2 = agent2.call(userMsg2).block();
        System.out.println("Agent: " + reply2.getTextContent());
    }
}
