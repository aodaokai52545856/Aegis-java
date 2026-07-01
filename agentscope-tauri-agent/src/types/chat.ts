export type ChatMode = "chat" | "agent";

export type SseEventType =
  | "text_delta"
  | "thinking_delta"
  | "tool_start"
  | "tool_result"
  | "done"
  | "error";

export interface SseEvent {
  type: SseEventType;
  content?: string;
  tool?: string;
  args?: unknown;
  message?: string;
  requestId?: string;
  keyId?: number;
}

export interface ModelInfo {
  id: string;
  name: string;
}

export type TraceStepType = "thinking" | "tool_call" | "tool_result";

export interface TraceStep {
  id: string;
  type: TraceStepType;
  tool?: string;
  content: string;
  status?: "running" | "done";
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant" | "tool";
  content: string;
  toolName?: string;
  streaming?: boolean;
  /** Agent 模式：思考 / 工具调用轨迹，显示在总结之前 */
  trace?: TraceStep[];
}

export interface ChatStreamRequest {
  message: string;
  model: string;
  sessionId: string;
}
