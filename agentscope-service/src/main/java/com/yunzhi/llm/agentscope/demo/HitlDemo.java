package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HitlDemo {

    public static class Tools {
        @Tool(name = "delete_file", description = "Delete a file")
        public String del(@ToolParam(name = "filename") String f) {
            return "deleted " + f;
        }

        @Tool(name = "search_web", description = "Search the web")
        public String search(@ToolParam(name = "q") String q) {
            return "results for " + q;
        }
    }

    public static void main(String[] args) {
        String apiKey = "sk-d221480c7a4c4b5aa45f67fa800e5da6";

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new Tools());

        PermissionContextState permissionContext = PermissionContextState.builder()
                .addAskRule("tool", new PermissionRule(
                        "delete_file", "*", PermissionBehavior.ASK, "hitl-demo"))
                .build();

        ReActAgent agent = ReActAgent.builder()
                .name("SafeAgent")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-plus").stream(true)
                        .formatter(new DashScopeChatFormatter()).build())
                .toolkit(toolkit)
                .permissionContext(permissionContext)
                .build();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("You: ");
            Msg userMsg = Msg.builder().role(MsgRole.USER)
                    .content(TextBlock.builder().text(sc.nextLine()).build()).build();
            Msg resp = agent.call(userMsg).block();

            while (resp != null && resp.getGenerateReason() == GenerateReason.PERMISSION_ASKING) {
                List<ToolUseBlock> pending = resp.getContentBlocks(ToolUseBlock.class);
                System.out.println("⚠ Pending tools: " + pending);
                System.out.print("Confirm? (y/n): ");
                boolean confirmed = sc.nextLine().equalsIgnoreCase("y");

                List<ConfirmResult> results = pending.stream()
                        .map(tool -> new ConfirmResult(confirmed, tool))
                        .toList();
                Msg confirmMsg = Msg.builder()
                        .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, results))
                        .build();
                resp = agent.call(confirmMsg).block();
            }

            if (resp != null) {
                System.out.println("Agent: " + resp.getTextContent());
            }
        }
    }
}
