import type { ChatMode, ChatStreamRequest, ModelInfo, SseEvent } from "../types/chat";

export async function fetchModels(baseUrl: string): Promise<ModelInfo[]> {
  const response = await fetch(`${baseUrl}/api/v1/models`);
  if (!response.ok) {
    throw new Error(`加载模型失败 (${response.status})`);
  }
  return response.json();
}

interface StreamChatOptions {
  baseUrl: string;
  mode: ChatMode;
  request: ChatStreamRequest;
  signal?: AbortSignal;
  onEvent: (event: SseEvent) => void;
}

export async function streamChat(options: StreamChatOptions): Promise<void> {
  const path = options.mode === "agent" ? "/api/v1/agent/stream" : "/api/v1/chat/stream";
  const response = await fetch(`${options.baseUrl}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(options.request),
    signal: options.signal,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(parseHttpError(text) || `HTTP ${response.status}`);
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error("服务未返回数据流");
  }

  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed.startsWith("data:")) {
        if (trimmed.startsWith("{")) {
          parseAndEmit(trimmed, options.onEvent);
        }
        continue;
      }
      const payload = trimmed.slice(5).trim();
      if (payload) {
        parseAndEmit(payload, options.onEvent);
      }
    }
  }

  const tail = buffer.trim();
  if (tail.startsWith("data:")) {
    parseAndEmit(tail.slice(5).trim(), options.onEvent);
  } else if (tail.startsWith("{")) {
    parseAndEmit(tail, options.onEvent);
  }
}

export function parseHttpError(text: string): string {
  try {
    const json = JSON.parse(text) as { error?: string; message?: string };
    return json.error ?? json.message ?? text;
  } catch {
    return text.trim();
  }
}

function parseAndEmit(payload: string, onEvent: (event: SseEvent) => void) {
  try {
    const event = JSON.parse(payload) as SseEvent;
    onEvent(event);
  } catch {
    /* ignore malformed chunks */
  }
}
