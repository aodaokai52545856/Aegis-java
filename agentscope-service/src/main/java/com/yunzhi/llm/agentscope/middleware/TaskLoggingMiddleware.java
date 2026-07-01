package com.yunzhi.llm.agentscope.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.state.Task;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

/** 在推理前后打印任务列表变化（替代 1.x PlanNotebook 的 changeHook）。 */
public class TaskLoggingMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext ctx,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {
        logTasks("before", agent);
        return next.apply(input).doOnComplete(() -> logTasks("after", agent));
    }

    private static void logTasks(String phase, Agent agent) {
        List<Task> tasks = agent.getAgentState().getTasksContext().getTasks();
        if (!tasks.isEmpty()) {
            System.out.println("\n>>> Tasks " + phase + " reasoning:");
            tasks.forEach(task ->
                    System.out.printf("  - [%s] %s: %s%n",
                            task.getState(), task.getSubject(), task.getDescription()));
        }
    }
}
