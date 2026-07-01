# AgentScope Java 2.0 迁移说明

本文档说明本项目从 AgentScope 1.x 迁移到 **2.0.0-RC4** 时的 API 差异、设计意图，以及 `agentscope-service` 模块中各 Demo 的对应改写方式。

当前依赖版本：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-spring-boot-starter</artifactId>
    <version>2.0.0-RC4</version>
</dependency>
```

---

## 一、为什么要改 API？

AgentScope 2.0 不是简单的重命名，而是一次面向生产场景的架构升级，核心目标：

| 目标 | 说明 |
|------|------|
| **更可组合** | 扩展点从「监听所有事件」变为「在正确层级介入」 |
| **状态更清晰** | 对话、任务、权限、工具上下文统一收敛到 `AgentState` |
| **扩展更稳定** | Middleware 洋葱模型，职责分离，未实现的 hook 零开销跳过 |
| **生产可用** | 权限/HITL、状态持久化、追踪等成为一等公民 |

---

## 二、核心 API 对照表

### 2.1 Hook → Middleware

| 1.x（已废弃） | 2.0（推荐） |
|---------------|-------------|
| `implements Hook` | `implements MiddlewareBase` |
| `.hook(hook)` / `.hooks(list)` | `.middleware(mw)` / `.middlewares(list)` |
| `onEvent(HookEvent)` + `instanceof` | 按阶段实现对应方法（见下表） |
| `PreCallEvent` / `PostCallEvent` | `onAgent` + `AgentInput` |
| `PreReasoningEvent` / `PostReasoningEvent` | `onReasoning` + `ReasoningInput` |
| `PreActingEvent` / `PostActingEvent` | `onActing` + `ActingInput` |
| 模型调用前后拦截 | `onModelCall` + `ModelCallInput` |
| 修改系统提示词 | `onSystemPrompt` |

**Middleware 五个挂载点：**

```
onAgent          — 包裹完整一次对话（最外层）
  └── ReAct 循环
        ├── onReasoning
        │     ├── onSystemPrompt（变换系统提示词）
        │     └── onModelCall（模型 API 调用）
        └── onActing（工具执行）
```

洋葱模型：调用 `next.apply(input)` 进入下一层，可在前后插入逻辑，或用 Reactor 操作符观测事件流。

**本项目示例：**

- `middleware/LoggingMiddleware.java` — 替代原 `LoggingHook`，在 `onAgent` 中打印日志并修改输入
- `middleware/TaskLoggingMiddleware.java` — 在 `onReasoning` 前后打印任务列表变化

---

### 2.2 Memory → AgentState

| 1.x | 2.0 |
|-----|-----|
| `InMemoryMemory` 注入 Agent | 对话历史由内置 `AgentState.context` 维护 |
| `.memory(memory)` | **Builder 已移除**，无需显式注入 |
| `memory.getMessages()` | `agent.getAgentState().getContext()` |
| `agent.getMemory()` | `agent.getAgentState()` |

`AgentState` 除对话上下文外，还统一管理：

- `tasksContext` — 任务列表
- `permissionContext` — 权限规则
- `toolContext` — 工具执行上下文
- `summary` — 上下文摘要
- 其他运行时字段

---

### 2.3 Session 持久化 → AgentStateStore

| 1.x | 2.0 |
|-----|-----|
| `JsonSession` + `Session` | `JsonFileAgentStateStore`（实现 `AgentStateStore`） |
| `SessionManager` | 直接使用 `AgentStateStore` + Agent 方法 |
| `agent.loadIfExists(session, sessionId)` | `agent.getAgentState(userId, sessionId)`（自动加载或创建） |
| `agent.saveTo(session, sessionId)` | `agent.saveAgentState(userId, sessionId)` |
| — | `.stateStore(store)` + `.defaultSessionId(id)` 配置 Builder |

**典型写法：**

```java
String sessionId = "user_hollis_session";
AgentStateStore stateStore = new JsonFileAgentStateStore(sessionPath);

ReActAgent agent = ReActAgent.builder()
        .stateStore(stateStore)
        .defaultSessionId(sessionId)
        .build();

boolean resumed = stateStore.exists(null, sessionId);
agent.getAgentState();

// ... 多轮对话 ...

agent.saveAgentState(null, sessionId);
```

---

### 2.4 人机确认（HITL）

| 1.x | 2.0 |
|-----|-----|
| 自定义 `Hook` 监听 `PostReasoningEvent` | `PermissionContextState` 声明规则 |
| `e.stopAgent()` 暂停 | 框架返回 `GenerateReason.PERMISSION_ASKING` |
| 检查 `ToolUseBlock` 判断待确认 | 检查 `resp.getGenerateReason() == PERMISSION_ASKING` |
| `agent.call()` 继续 / `agent.call(cancelMsg)` 拒绝 | `Msg.METADATA_CONFIRM_RESULTS` + `ConfirmResult` |

**典型写法：**

```java
PermissionContextState permissionContext = PermissionContextState.builder()
        .addAskRule("tool", new PermissionRule(
                "delete_file", "*", PermissionBehavior.ASK, "hitl-demo"))
        .build();

ReActAgent agent = ReActAgent.builder()
        .permissionContext(permissionContext)
        .build();

// 用户确认后
List<ConfirmResult> results = pending.stream()
        .map(tool -> new ConfirmResult(confirmed, tool))
        .toList();
Msg confirmMsg = Msg.builder()
        .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, results))
        .build();
agent.call(confirmMsg).block();
```

---

### 2.5 计划 / 任务

| 1.x | 2.0 |
|-----|-----|
| `PlanNotebook` + `planNotebook(nb)` | `enableTaskList(true)` |
| `nb.addChangeHook(...)` 监听计划变化 | `TaskLoggingMiddleware` 或读取 `getTasksContext()` |
| `enablePlan()` | `enableTaskList(true)`（自动注册 `TodoTools` + `TaskReminderMiddleware`） |

---

### 2.6 RAG

| 1.x | 2.0 |
|-----|-----|
| `.ragMode(RAGMode.GENERIC)` + 内部 Hook | 同上，框架自动注入 RAG Middleware |
| 注释「自动 Hook」 | 语义改为「自动注入 RAG Middleware」 |

配置方式不变，底层实现从 Hook 迁移到 Middleware 体系。

---

### 2.7 GenerateReason 枚举（常用值）

| 值 | 含义 |
|----|------|
| `MODEL_STOP` | 正常完成 |
| `TOOL_CALLS` | 模型返回工具调用（框架继续执行） |
| `PERMISSION_ASKING` | 等待用户确认工具（2.0 HITL） |
| `REASONING_STOP_REQUESTED` | Hook 在推理阶段暂停（1.x HITL，已废弃） |
| `ACTING_STOP_REQUESTED` | Hook 在工具执行后暂停（1.x HITL，已废弃） |
| `MIDDLEWARE_STOP_REQUESTED` | Middleware 请求停止 |
| `INTERRUPTED` | 被中断 |
| `MAX_ITERATIONS` | 达到最大迭代次数 |

---

## 三、2.0 执行模型

```
用户消息
   │
   ▼
onAgent ─────────────────────────────────────────┐
   │                                              │
   ├── ReAct 循环                                │
   │     ├── onReasoning                         │
   │     │     ├── onSystemPrompt（变换提示词）   │
   │     │     └── onModelCall（调模型）          │
   │     └── onActing（执行工具，含 Permission）  │
   │                                              │
   ▼                                              │
AgentState（context / tasks / permission / ...）  │
   │                                              │
   ▼                                              │
持久化 AgentStateStore ◄──────────────────────────┘
```

---

## 四、本项目 Demo 改写清单

| Demo | 主要变更 |
|------|----------|
| `HookChatDemo` | `.hook(LoggingHook)` → `.middleware(LoggingMiddleware)` |
| `HitlDemo` | 自定义 `ConfirmHook` → `PermissionContextState` + `ConfirmResult` |
| `MultiTurnChatDemo` | 移除 `InMemoryMemory`，用 `getAgentState().getContext()` 查看历史 |
| `PersistentChatDemo` | `JsonSession` → `JsonFileAgentStateStore` + `saveAgentState` |
| `SessionManagerChatDemo` | 移除 `SessionManager`，改为 `AgentStateStore` 模式 |
| `McpClientDemo` | 移除废弃的 `.memory()` |
| `InterruptionDemo` | `getMemory()` → `getAgentState().getContext()` |
| `PlanExecuteDemo` | `PlanNotebook` → `enableTaskList(true)` + `TaskLoggingMiddleware` |
| `FullMemoryDemo` | 移除 `AutoContextHook`（RC4 核心包未包含），改用 `AgentState` 持久化 |
| `BailianAgentControlLongTermMemoryDemo` | 移除 `.memory()`，修复 `agent2` 调用错误 |
| `BailianLongTermMemoryDemo` | 本身已兼容 2.0，无需改动 |
| `BailianRagDemo` / `LocalRagDemo` | 仅更新注释，配置方式不变 |

---

## 五、已知限制（2.0.0-RC4）

### AutoContextMemory / AutoContextHook

`FullMemoryDemo` 原使用的 `AutoContextMemory` 与 `AutoContextHook` **尚未纳入 2.0.0-RC4 核心包**，需等待官方扩展模块 `agentscope-extensions-autocontext-memory` 发布后再接入。

当前 `FullMemoryDemo` 采用：

- 百炼长期记忆（`BailianLongTermMemory`）
- `JsonFileAgentStateStore` 短期状态持久化
- `enableTaskList(true)` 任务列表

### 仍保留的废弃 API

框架内部及部分扩展（如 `GenericRAGHook`、`StaticLongTermMemoryHook`）仍使用旧 Hook 接口，通过 `LegacyHookDispatcher` 兼容。用户自定义代码应优先使用 Middleware，避免依赖即将移除的 Hook。

---

## 六、代码示例速查

### 自定义日志 Middleware

```java
public class LoggingMiddleware implements MiddlewareBase {
    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent, RuntimeContext ctx, AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        System.out.println("[Middleware] Agent " + agent.getName() + " starting...");
        return next.apply(input)
                .doOnComplete(() ->
                        System.out.println("[Middleware] Agent " + agent.getName() + " finished."));
    }
}
```

### 注册 Middleware

```java
ReActAgent agent = ReActAgent.builder()
        .name("Assistant")
        .model(model)
        .middleware(new LoggingMiddleware())
        .build();
```

### 多轮对话（无持久化）

```java
ReActAgent agent = ReActAgent.builder()
        .name("Assistant")
        .model(model)
        .build();

agent.call(msg1).block();
agent.call(msg2).block();

int count = agent.getAgentState().getContext().size();
```

---

## 七、参考链接

- [Middleware 官方文档](https://java.agentscope.io/v2/en/docs/building-blocks/middleware.html)
- [AgentScope Java 2.0 介绍](https://java.agentscope.io/v2/en/docs/index.html)
- [Changelog](https://docs.agentscope.io/v2/change-log)

---

## 八、相关源码位置

```
agentscope-service/src/main/java/com/yunzhi/llm/agentscope/
├── middleware/
│   ├── LoggingMiddleware.java      # 日志观测
│   └── TaskLoggingMiddleware.java  # 任务列表变化日志
└── demo/                           # 各场景示例（均已按 2.0 改写）
```

## 相关文档

- [Tauri Agent 联调指南](agentscope-tauri-agent.md)
