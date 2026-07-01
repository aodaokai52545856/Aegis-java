package com.yunzhi.llm.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.reader.PDFReader;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.store.InMemoryStore;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LocalRagDemo {
    public static void main(String[] args) throws IOException {
        String apiKey = "sk-d221480c7a4c4b5aa45f67fa800e5da6";

        // 1. 起 Embedding + 内存向量库
        EmbeddingModel embed = DashScopeTextEmbedding.builder()
                .apiKey(apiKey).modelName("text-embedding-v3").dimensions(1024).build();
        InMemoryStore store = InMemoryStore.builder().dimensions(1024).build();
        SimpleKnowledge knowledge = SimpleKnowledge.builder()
                .embeddingModel(embed).embeddingStore(store).build();

        // 2. 灌数据（PDF）
        PDFReader reader = new PDFReader();
        File file = new File("/Users/hollis/LLM课程视频/RAG材料/Java八股文介绍.pdf");
        List<Document> docs = reader.read(ReaderInput.fromPath("/Users/hollis/LLM课程视频/RAG材料/Java八股文介绍.pdf")).block();
        knowledge.addDocuments(docs).block();

        // 3. 起 Agent，自动注入
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
