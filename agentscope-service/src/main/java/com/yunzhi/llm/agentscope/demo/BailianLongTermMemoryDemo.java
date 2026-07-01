package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.bailian.BailianLongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;

public class BailianLongTermMemoryDemo {

    public static void main(String[] args) {
        // 1. 配置 bailian 长期记忆

        BailianLongTermMemory longTermMemory = BailianLongTermMemory.builder()
                .userId("hollis666")           // 关键：每个用户一个隔离的记忆空间
                .apiKey("sk-dcebc45c03b04c6e85391abb2264e594")
                .memoryLibraryId("8553b56bbeb9451295e49a09d8c26ee3")
                .build();

        // 2. 创建 Agent，启用 STATIC_CONTROL 模式
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .model(DashScopeChatModel.builder()
                        .apiKey("sk-dcebc45c03b04c6e85391abb2264e594")
                        .modelName("qwen-plus")
                        .build())
                .longTermMemory(longTermMemory)
                .longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL)
                .build();

        // 3. 模拟首次对话：告诉 Agent 一些用户偏好
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("我是Hollis，我不喜欢吃香菜，我爱吃辣的").build())
                .build();

        Msg reply1 = agent.call(userMsg).block();
        System.out.println("Agent: " + reply1.getTextContent());

        // ===模拟 JVM 重启后的情况——>新建 Agent 实例（短期记忆全空）===
        // 但只要 userId 一致，长期记忆中的偏好仍可被检索到

        BailianLongTermMemory longTermMemory2 = BailianLongTermMemory.builder()
                .userId("hollis666")           // 同样的 userId
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
                .longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL)
                .build();

        // 提问 - Agent 会自动检索到之前的过敏信息

        Msg userMsg1 = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("我来杭州了，请帮我推荐几个餐馆吧").build())
                .build();

        Msg reply3 = agent2.call(userMsg1).block();
        System.out.println("Agent: " + reply3.getTextContent());
    }
}
