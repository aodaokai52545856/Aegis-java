package com.yunzhi.llm.agentscope.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

public class LoggingMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext ctx,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        System.out.println("[Middleware] Agent " + agent.getName() + " starting...");

        List<Msg> inputMessages = List.of(
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text("你好，你认为我现在最大的烦恼是什么？").build())
                        .build());
        AgentInput modified = new AgentInput(inputMessages);

        return next.apply(modified)
                .doOnComplete(() ->
                        System.out.println("[Middleware] Agent " + agent.getName() + " finished."));
    }
}
