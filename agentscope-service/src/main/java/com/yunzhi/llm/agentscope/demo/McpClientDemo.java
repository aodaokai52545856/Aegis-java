package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.time.Duration;

/**
 * AgentScope 调用 MCP Server (SSE) 的示例。
 * <p>
 * 前置条件：先启动 mcp-server-sse 模块（端口 8003），提供天气查询工具 getWeather。
 */
public class McpClientDemo {

    public static void main(String[] args) {
        String apiKey = "sk-8996fd8e3d6f42359b0cd2d8cd3a656b";

        McpClientWrapper mcpClient = McpClientBuilder.create("weather-mcp")
                .sseTransport("http://127.0.0.1:8003/sse")
                .timeout(Duration.ofSeconds(30))
                .buildAsync()
                .block();

        System.out.println("✅ 已成功连接到 MCP Server (SSE)");

        Toolkit toolkit = new Toolkit();
        toolkit.registerMcpClient(mcpClient).block();
        System.out.println("📦 已注册的工具列表: " + toolkit.getToolNames());

        ReActAgent agent = ReActAgent.builder()
                .name("WeatherAssistant")
                .sysPrompt("你是一个天气助手，可以帮用户查询各个城市的天气信息。请使用工具来获取天气数据。")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-max")
                        .build())
                .toolkit(toolkit)
                .build();

        System.out.println("\n--- 第1轮对话 ---");
        Msg reply1 = agent.call(Msg.builder()
                .textContent("北京今天天气怎么样？")
                .build()).block();
        System.out.println("用户: 北京今天天气怎么样？");
        System.out.println("Agent: " + reply1.getTextContent());

        System.out.println("\n--- 第2轮对话 ---");
        Msg reply2 = agent.call(Msg.builder()
                .textContent("深圳呢？")
                .build()).block();
        System.out.println("用户: 深圳呢？");
        System.out.println("Agent: " + reply2.getTextContent());

        toolkit.removeMcpClient("weather-mcp").block();
        System.out.println("\n🔌 已断开 MCP 连接");
    }
}
