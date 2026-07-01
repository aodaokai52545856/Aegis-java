package com.yunzhi.llm.agentscope.demo;

import com.yunzhi.llm.agentscope.middleware.TaskLoggingMiddleware;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class PlanExecuteDemo {

    static Map<String, String> files = new HashMap<>();

    @Tool(name = "write_file", description = "Write content to a file")
    public Mono<String> write(@ToolParam(name = "name") String n,
                              @ToolParam(name = "content") String c) {
        files.put(n, c);
        return Mono.just("Saved " + n);
    }

    @Tool(name = "read_file", description = "Read content from a file")
    public Mono<String> read(@ToolParam(name = "name") String n) {
        return Mono.just(files.getOrDefault(n, "(not found)"));
    }

    @Tool(name = "calc", description = "Evaluate a simple math expression like 10*5")
    public Mono<String> calc(@ToolParam(name = "expr") String e) {
        String[] parts = e.split("\\*");
        return Mono.just(e + " = " + (Integer.parseInt(parts[0].trim())
                * Integer.parseInt(parts[1].trim())));
    }

    public static void main(String[] args) {
        String apiKey = "sk-d221480c7a4c4b5aa45f67fa800e5da6";
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new PlanExecuteDemo());

        // 2.0：enableTaskList 自动注册 TodoTools + TaskReminderMiddleware，替代 PlanNotebook
        ReActAgent agent = ReActAgent.builder()
                .name("Planner")
                .sysPrompt("You break down complex tasks into a plan and execute step by step.")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-plus")
                        .stream(true)
                        .formatter(new DashScopeChatFormatter()).build())
                .toolkit(toolkit)
                .maxIters(50)
                .enableTaskList(true)
                .middleware(new TaskLoggingMiddleware())
                .build();

        Msg user = Msg.builder().role(MsgRole.USER).content(TextBlock.builder().text(
                "Calculate 10*5, save the result to result.txt, then read it back to verify. " +
                        "This is a multi-step task, please plan first."
        ).build()).build();

        Msg resp = agent.call(user).block();
        System.out.println("\nFinal: " + resp.getTextContent());
    }
}
