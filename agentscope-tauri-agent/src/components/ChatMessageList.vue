<script setup lang="ts">
import { ref } from "vue";
import { useChatStore } from "../stores/chat";
import TraceStepBlock from "./TraceStepBlock.vue";

const emit = defineEmits<{ useHint: [text: string] }>();

const chat = useChatStore();
const copiedId = ref<string | null>(null);
const threadRef = ref<HTMLElement | null>(null);

async function copyText(id: string, text: string) {
  try {
    await navigator.clipboard.writeText(text);
    copiedId.value = id;
    setTimeout(() => {
      if (copiedId.value === id) copiedId.value = null;
    }, 1500);
  } catch {
    /* ignore */
  }
}

function scrollToBottom() {
  const el = threadRef.value;
  if (el) el.scrollTop = el.scrollHeight;
}

defineExpose({ scrollToBottom });
</script>

<template>
  <div ref="threadRef" class="thread">
    <div v-if="chat.messages.length === 0" class="empty">
      <div class="empty-badge">∞ Agent</div>
      <h2>向 Agent 提问</h2>
      <p>推理、工具调用与 MCP 会显示在最终回复之前。</p>
      <div class="hints">
        <button type="button" class="hint" @click="emit('useHint', '现在几点了？')">
          现在几点了？
        </button>
        <button type="button" class="hint" @click="emit('useHint', '帮我查当前时间')">
          帮我查当前时间
        </button>
      </div>
    </div>

    <div v-else class="messages">
      <article
        v-for="msg in chat.messages"
        :key="msg.id"
        class="turn"
        :class="msg.role"
      >
        <header v-if="msg.role === 'user'" class="turn-head user-head">
          <span class="avatar user-avatar">You</span>
        </header>

        <header v-else-if="msg.role === 'assistant'" class="turn-head">
          <span class="avatar agent-avatar">∞</span>
          <span class="agent-title">Agent</span>
          <span v-if="msg.streaming" class="live">生成中</span>
          <button
            v-if="msg.content"
            type="button"
            class="copy-btn"
            @click="copyText(msg.id, msg.content)"
          >
            {{ copiedId === msg.id ? "已复制" : "复制" }}
          </button>
        </header>

        <div v-if="msg.role === 'user'" class="user-body">
          {{ msg.content }}
        </div>

        <template v-else-if="msg.role === 'assistant'">
          <div v-if="msg.trace && msg.trace.length > 0" class="trace-list">
            <TraceStepBlock v-for="step in msg.trace" :key="step.id" :step="step" />
          </div>

          <div v-if="msg.content" class="answer">
            {{ msg.content }}<span v-if="msg.streaming" class="cursor">▍</span>
          </div>
          <div v-else-if="msg.streaming" class="answer waiting">
            <span class="cursor">▍</span>
          </div>
        </template>
      </article>
    </div>
  </div>
</template>

<style scoped>
.thread {
  flex: 1;
  overflow-y: auto;
  padding: var(--panel-pad);
}

.empty {
  padding: 48px var(--panel-pad) 24px;
  max-width: 420px;
}

.empty-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--accent);
  background: var(--accent-soft);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
}

.empty h2 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty p {
  margin: 0 0 16px;
  color: var(--text-muted);
  font-size: 12px;
}

.hints {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hint {
  font-size: 12px;
  color: var(--text-secondary);
  padding: 8px 10px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--bg-panel);
  text-align: left;
  transition: border-color 0.15s, background 0.15s;
}

.hint:hover {
  border-color: rgba(55, 148, 255, 0.35);
  background: var(--bg-hover);
  color: var(--text-primary);
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.turn {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.turn-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 22px;
}

.user-head {
  justify-content: flex-end;
}

.avatar {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-avatar {
  background: var(--bg-surface);
  color: var(--text-secondary);
  border: 1px solid var(--border-subtle);
}

.agent-avatar {
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
}

.agent-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.live {
  font-size: 11px;
  color: var(--accent);
  animation: fade 1.4s ease-in-out infinite;
}

.copy-btn {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-muted);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.copy-btn:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.user-body {
  align-self: flex-end;
  max-width: 92%;
  padding: 8px 12px;
  background: var(--bg-user);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-primary);
}

.trace-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin: 2px 0 8px 30px;
}

.answer {
  margin-left: 30px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.answer.waiting {
  color: var(--text-muted);
}

.cursor {
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

@keyframes fade {
  50% {
    opacity: 0.45;
  }
}
</style>
