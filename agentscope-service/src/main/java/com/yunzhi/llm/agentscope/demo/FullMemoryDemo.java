package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.bailian.BailianLongTermMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 综合记忆示例：长期记忆（百炼）+ 短期上下文（AgentState）+ 任务列表 + 状态持久化。
 * <p>
 * 2.0 RC4 中 AutoContextMemory/AutoContextHook 尚未纳入核心包，上下文压缩需等待官方扩展模块。
 */
public class FullMemoryDemo {

    private static final String SESSION_ID = "user_hollis_session";

    public static void main(String[] args) {
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .apiKey("sk-dcebc45c03b04c6e85391abb2264e594")
                .modelName("qwen3-max")
                .stream(true)
                .enableThinking(true)
                .formatter(new DashScopeChatFormatter())
                .defaultOptions(GenerateOptions.builder().thinkingBudget(1024).build())
                .build();

        BailianLongTermMemory longTermMemory = BailianLongTermMemory.builder()
                .userId("hollis666")
                .apiKey("sk-dcebc45c03b04c6e85391abb2264e594")
                .memoryLibraryId("8553b56bbeb9451295e49a09d8c26ee3")
                .build();

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ReadFileTool());
        toolkit.registerTool(new WriteFileTool());

        Path sessionPath = Paths.get(System.getProperty("user.home"),
                ".agentscope", "examples", "sessions");
        AgentStateStore stateStore = new JsonFileAgentStateStore(sessionPath);

        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant")
                .model(chatModel)
                .maxIters(50)
                .longTermMemory(longTermMemory)
                .longTermMemoryMode(LongTermMemoryMode.STATIC_CONTROL)
                .enableTaskList(true)
                .toolkit(toolkit)
                .stateStore(stateStore)
                .defaultSessionId(SESSION_ID)
                .build();

        agent.getAgentState();

        try {
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text("...").build())
                    .build();
            Msg response = agent.call(userMsg).block();
            System.out.println(response.getTextContent());
            agent.saveAgentState(null, SESSION_ID);
        } catch (Throwable e) {
            agent.saveAgentState(null, SESSION_ID);
            throw e;
        }
    }
}
