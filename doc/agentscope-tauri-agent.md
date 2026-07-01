# Aegis Tauri Agent 联调指南

本文档说明 `agentscope-tauri-agent` 桌面客户端与 `agentscope-service` 后端的接入方式。

## 架构

```
agentscope-tauri-agent (Tauri 2 + Vue 3)
        │ HTTP/SSE
        ▼
agentscope-service :8010
        │ 动态分配 API Key
        ▼
DeepSeek API (OpenAI 兼容)
```

## 启动顺序

### 1. 启动后端

```bash
cd agentscope-service
mvn spring-boot:run
```

服务默认监听 `http://127.0.0.1:8010`。

SQLite 数据库路径：`~/.agentscope/aegis-keys.db`

### 2. 注册 DeepSeek API Key

```bash
curl -X POST http://127.0.0.1:8010/api/v1/admin/keys \
  -H "Content-Type: application/json" \
  -H "X-Admin-Token: change-me-in-production" \
  -d '{
    "label": "deepseek-main",
    "provider": "DEEPSEEK",
    "secret": "sk-your-deepseek-key",
    "enabled": true,
    "priority": 10
  }'
```

查看 Key 列表（secret 已脱敏）：

```bash
curl http://127.0.0.1:8010/api/v1/admin/keys \
  -H "X-Admin-Token: change-me-in-production"
```

### 3. 启动客户端

```bash
cd agentscope-tauri-agent
npm install
npm run tauri:dev
```

仅 Web 调试（不启动 Tauri 壳）：

```bash
npm run dev
```

## API 契约

### 模型列表

`GET /api/v1/models`

```json
[
  { "id": "deepseek-chat", "name": "DeepSeek Chat" },
  { "id": "deepseek-reasoner", "name": "DeepSeek Reasoner" }
]
```

### 普通流式对话

`POST /api/v1/chat/stream`  
`Content-Type: application/json`  
`Accept: text/event-stream`

```json
{
  "message": "你好",
  "model": "deepseek-chat",
  "sessionId": "optional-uuid"
}
```

### Agent 流式对话（含工具）

`POST /api/v1/agent/stream`  
请求体同上。服务端注册 `SimpleTools`（如 `get_time`）。

### SSE 事件格式

每行一个 JSON 对象（Spring 会以 `data: {...}` 形式推送）：

| type | 说明 | 字段 |
|------|------|------|
| `text_delta` | 文本增量 | `content` |
| `tool_start` | 工具调用开始 | `tool`, `args` |
| `tool_result` | 工具执行结果 | `tool`, `content` |
| `done` | 流结束 | `requestId`, `keyId` |
| `error` | 错误 | `message` |

示例：

```json
{"type":"text_delta","content":"你好"}
{"type":"done","requestId":"uuid","keyId":1}
```

## API Key 动态分配

每次 `/chat/stream` 或 `/agent/stream` 请求：

1. `ApiKeyPoolService` 从 SQLite 中选取 `enabled=true` 的 Key（按 `priority` 降序，同优先级轮询）
2. 写入 `api_key_usages` 表，记录 `requestId`、`sessionId`、`model`、`mode`
3. 使用分配的 Key 调用 DeepSeek

便于后续按 `keyId` / `requestId` 做用量统计。

## Admin API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/keys` | 列表 |
| POST | `/api/v1/admin/keys` | 创建 |
| PUT | `/api/v1/admin/keys/{id}` | 更新 |
| DELETE | `/api/v1/admin/keys/{id}` | 删除 |

所有 Admin 请求需 Header：`X-Admin-Token: <aegis.admin-token>`

## 客户端功能

- 模型下拉选择（DeepSeek Chat / Reasoner）
- 模式切换：普通对话 / Agent（工具）
- SSE 流式渲染
- `sessionId` 持久化（localStorage），支持多轮上下文
- 设置页配置 `serviceBaseUrl`（默认 `http://127.0.0.1:8010`）

## 配置项

`agentscope-service/src/main/resources/application.yml`：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | 8010 | HTTP 端口 |
| `aegis.admin-token` | change-me-in-production | Admin API 令牌 |
| `aegis.deepseek.base-url` | https://api.deepseek.com | DeepSeek 端点 |
| `aegis.cors.allowed-origins` | localhost:1420, tauri://localhost | CORS |

## 废弃接口

`GET /stream/chat` 已标记 `@Deprecated`，请使用 `POST /api/v1/chat/stream`。

## 已知限制（MVP）

- API Key 明文存储于 SQLite，生产环境需加密或接入密钥管理服务
- Agent 会话按 `sessionId` 缓存 `ReActAgent`，同会话内复用首次分配的 Key
- 普通对话会话历史存于内存，服务重启后丢失
- 需要 JDK 21+ 编译运行 `agentscope-service`（项目 POM 目标 Java 25）
