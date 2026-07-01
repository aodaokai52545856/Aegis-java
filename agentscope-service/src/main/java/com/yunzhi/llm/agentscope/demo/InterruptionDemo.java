package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;

public class InterruptionDemo {

    public static void main(String[] args) throws Exception {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new SlowTools());

        ReActAgent agent = ReActAgent.builder()
                .name("DataAgent")
                .sysPrompt("You are a data processing assistant. "
                        + "Use the process_large_dataset tool to process datasets.")
                .model(DashScopeChatModel.builder()
                        .apiKey("sk-dcebc45c03b04c6e85391abb2264e594").modelName("qwen-max").stream(false).build())
                .toolkit(toolkit)
                .maxIters(10)
                .build();

        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder()
                        .text("Process the 'orders' dataset with 'aggregate' operation.")
                        .build())
                .build();

        Thread agentThread = new Thread(() -> {
            Msg response = agent.call(userMsg).block();
            System.out.println("[Agent] " + response.getTextContent());
        });
        agentThread.start();

        Thread.sleep(2000);
        System.out.println(">>> USER INTERRUPTS <<<");

        Msg interruptMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder()
                        .text("Stop! I need to change parameters.")
                        .build())
                .build();
        agent.interrupt(interruptMsg);

        agentThread.join();
        System.out.println("Context size: " + agent.getAgentState().getContext().size());
    }

    public static class SlowTools {
        @Tool(name = "process_large_dataset",
                description = "Process a large dataset (takes a long time)")
        public String processLargeDataset(
                @ToolParam(name = "dataset_name") String name,
                @ToolParam(name = "operation") String op,
                ToolEmitter emitter) {

            for (int i = 1; i <= 10; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Processing interrupted at " + (i * 10) + "%";
                }
                emitter.emit(ToolResultBlock.text("Progress: " + (i * 10) + "%"));
            }
            return "Done processing " + name;
        }
    }
}
