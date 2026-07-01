package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.integration.bailian.BailianConfig;
import io.agentscope.core.rag.integration.bailian.BailianKnowledge;

import java.io.IOException;

public class BailianRagDemo {
    public static void main(String[] args) throws IOException {
        String apiKey = "sk-d221480c7a4c4b5aa45f67fa800e5da6";

        BailianConfig config =
                BailianConfig.builder()
                        .accessKeyId("xxxx").accessKeySecret("xxxx").workspaceId("llm-hs8cun2smw9190xh")
                        .build();

        BailianKnowledge knowledge = BailianKnowledge.builder()
                .config(config)
                .indexId("8ugn8gjo3f")
                .build();

        ReActAgent agent = ReActAgent.builder()
                .name("FAQBot")
                .sysPrompt("基于检索到的知识回答用户问题；若没找到请明确告知。")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-max")
                        .build())
                .knowledge(knowledge)
                .ragMode(RAGMode.GENERIC)     // 框架自动注入 RAG Middleware
                .build();

        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("这份JAVA八股文有哪些内容？").build())
                .build();
        System.out.println(agent.call(msg).block().getTextContent());
    }
}
