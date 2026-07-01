import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { fetchModels, streamChat } from "../api/chat";
import type { ChatMessage, ChatMode, ModelInfo, SseEvent, TraceStep } from "../types/chat";
import { useSettingsStore } from "./settings";

const SESSION_KEY = "aegis.sessionId";

function newId(): string {
  return crypto.randomUUID();
}

function loadSessionId(): string {
  const existing = localStorage.getItem(SESSION_KEY);
  if (existing) {
    return existing;
  }
  const id = newId();
  localStorage.setItem(SESSION_KEY, id);
  return id;
}

function friendlyError(message: string): string {
  if (message.includes("No enabled API keys")) {
    return "服务端未配置 DeepSeek API Key。请在 application-local.yml 或环境变量 DEEPSEEK_API_KEY 中设置。";
  }
  return message;
}

function isInternalTool(name?: string): boolean {
  return !name || name === "__fragment__";
}

function formatToolArgs(args: unknown): string {
  if (args == null) return "";
  try {
    const text = JSON.stringify(args, null, 2);
    return text === "{}" ? "" : text;
  } catch {
    return String(args);
  }
}

function closeOpenThinking(trace: TraceStep[]) {
  const last = trace[trace.length - 1];
  if (last?.type === "thinking") {
    last.status = "done";
  }
}

function appendThinking(trace: TraceStep[], delta: string) {
  const last = trace[trace.length - 1];
  if (last?.type === "thinking" && last.status !== "done") {
    last.content += delta;
    return;
  }
  trace.push({
    id: newId(),
    type: "thinking",
    content: delta,
    status: "running",
  });
}

function handleAgentEvent(assistant: ChatMessage, event: SseEvent) {
  if (!assistant.trace) {
    assistant.trace = [];
  }
  const trace = assistant.trace;

  if (event.type === "thinking_delta" && event.content) {
    appendThinking(trace, event.content);
    return;
  }

  if (event.type === "tool_start" && !isInternalTool(event.tool)) {
    closeOpenThinking(trace);
    const argsText = formatToolArgs(event.args);
    trace.push({
      id: newId(),
      type: "tool_call",
      tool: event.tool,
      content: argsText ? `参数\n${argsText}` : "执行中…",
      status: "running",
    });
    return;
  }

  if (event.type === "tool_result" && !isInternalTool(event.tool)) {
    const pending = [...trace]
      .reverse()
      .find((s) => s.type === "tool_call" && s.tool === event.tool && s.status === "running");
    if (pending) {
      pending.status = "done";
      pending.content = event.content ?? pending.content;
    } else {
      trace.push({
        id: newId(),
        type: "tool_result",
        tool: event.tool,
        content: event.content ?? "",
        status: "done",
      });
    }
    return;
  }

  if (event.type === "text_delta" && event.content) {
    closeOpenThinking(trace);
    assistant.content += event.content;
  }
}

export const useChatStore = defineStore("chat", () => {
  const settingsStore = useSettingsStore();

  const messages = ref<ChatMessage[]>([]);
  const models = ref<ModelInfo[]>([]);
  const selectedModel = ref("");
  const mode = ref<ChatMode>("agent");
  const sessionId = ref(loadSessionId());
  const loading = ref(false);
  const error = ref<string | null>(null);
  const modelsLoading = ref(false);
  const abortController = ref<AbortController | null>(null);

  const canSend = computed(() => !loading.value && selectedModel.value !== "");

  const selectedModelName = computed(
    () => models.value.find((m) => m.id === selectedModel.value)?.name ?? selectedModel.value,
  );

  async function loadModels() {
    modelsLoading.value = true;
    error.value = null;
    try {
      const list = await fetchModels(settingsStore.settings.serviceBaseUrl);
      models.value = list;
      if (list.length > 0) {
        if (!list.find((m) => m.id === selectedModel.value)) {
          selectedModel.value = list[0].id;
        }
      } else {
        selectedModel.value = "";
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : "加载模型列表失败";
    } finally {
      modelsLoading.value = false;
    }
  }

  function resetSession() {
    sessionId.value = newId();
    localStorage.setItem(SESSION_KEY, sessionId.value);
    messages.value = [];
    error.value = null;
  }

  function stop() {
    abortController.value?.abort();
    abortController.value = null;
    loading.value = false;
  }

  async function send(text: string) {
    const trimmed = text.trim();
    if (!trimmed || loading.value || !canSend.value) {
      return;
    }

    error.value = null;
    loading.value = true;
    const isAgent = true; // chat 模式暂时禁用，固定走 Agent

    messages.value.push({
      id: newId(),
      role: "user",
      content: trimmed,
    });

    const assistantId = newId();
    messages.value.push({
      id: assistantId,
      role: "assistant",
      content: "",
      trace: isAgent ? [] : undefined,
      streaming: true,
    });

    const controller = new AbortController();
    abortController.value = controller;

    try {
      await streamChat({
        baseUrl: settingsStore.settings.serviceBaseUrl,
        mode: "agent",
        request: {
          message: trimmed,
          model: selectedModel.value,
          sessionId: sessionId.value,
        },
        signal: controller.signal,
        onEvent: (event) => {
          const assistant = messages.value.find((m) => m.id === assistantId);
          if (!assistant) return;

          if (isAgent) {
            handleAgentEvent(assistant, event);
          } else if (event.type === "text_delta" && event.content) {
            assistant.content += event.content;
          }

          if (event.type === "error") {
            error.value = friendlyError(event.message ?? "未知错误");
          }
        },
      });
    } catch (e) {
      if (!(e instanceof DOMException && e.name === "AbortError")) {
        error.value = friendlyError(e instanceof Error ? e.message : "请求失败");
      }
    } finally {
      const msg = messages.value.find((m) => m.id === assistantId);
      if (msg) {
        msg.streaming = false;
        if (msg.trace) {
          for (const step of msg.trace) {
            if (step.status === "running") {
              step.status = "done";
            }
          }
        }
        const hasOutput = msg.content || (msg.trace && msg.trace.length > 0);
        if (!hasOutput && error.value) {
          messages.value = messages.value.filter((m) => m.id !== assistantId);
        }
      }
      loading.value = false;
      abortController.value = null;
    }
  }

  return {
    messages,
    models,
    selectedModel,
    selectedModelName,
    mode,
    sessionId,
    loading,
    modelsLoading,
    error,
    canSend,
    loadModels,
    resetSession,
    stop,
    send,
  };
});
