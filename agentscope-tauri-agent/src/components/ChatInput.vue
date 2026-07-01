<script setup lang="ts">
import { ref } from "vue";
import { useChatStore } from "../stores/chat";

const chat = useChatStore();
const input = ref("");
const modelOpen = ref(false);
const textareaRef = ref<HTMLTextAreaElement | null>(null);

function setInput(text: string) {
  input.value = text;
}

defineExpose({ setInput, focus: () => textareaRef.value?.focus() });

async function submit() {
  const text = input.value;
  if (!text.trim() || !chat.canSend) return;
  input.value = "";
  modelOpen.value = false;
  await chat.send(text);
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    submit();
  }
  if (e.key === "Escape") {
    modelOpen.value = false;
  }
}

function pickModel(id: string) {
  chat.selectedModel = id;
  modelOpen.value = false;
}
</script>

<template>
  <div class="composer-dock">
    <div v-if="chat.loading" class="status-bar">
      <span class="status-dot" />
      Agent 正在回复…
    </div>

    <div class="composer">
      <textarea
        ref="textareaRef"
        v-model="input"
        rows="2"
        placeholder="向 Agent 提问…（Enter 发送，Shift+Enter 换行）"
        :disabled="!chat.canSend && !chat.loading"
        @keydown="onKeydown"
      />

      <div class="composer-bar">
        <div class="bar-left">
          <span class="mode-pill">∞ Agent</span>

          <div class="model-picker" :class="{ open: modelOpen }">
            <button
              type="button"
              class="model-btn"
              :disabled="chat.loading || chat.models.length === 0"
              @click="modelOpen = !modelOpen"
            >
              {{ chat.selectedModelName || "Model" }}
              <span class="chev">▾</span>
            </button>
            <ul v-if="modelOpen" class="model-menu">
              <li
                v-for="m in chat.models"
                :key="m.id"
                :class="{ active: chat.selectedModel === m.id }"
                @click="pickModel(m.id)"
              >
                {{ m.name }}
              </li>
            </ul>
          </div>
        </div>

        <div class="bar-right">
          <button
            v-if="chat.loading"
            type="button"
            class="action stop"
            title="Stop"
            @click="chat.stop()"
          >
            ■
          </button>
          <button
            v-else
            type="button"
            class="action send"
            :disabled="!input.trim() || !chat.canSend"
            title="Send"
            @click="submit"
          >
            ↑
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.composer-dock {
  flex-shrink: 0;
  padding: 0 var(--panel-pad) var(--panel-pad);
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-panel);
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 2px 8px;
  font-size: 11px;
  color: var(--text-muted);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  animation: pulse 1.2s ease-in-out infinite;
}

.composer {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-composer);
  overflow: hidden;
}

.composer:focus-within {
  border-color: rgba(55, 148, 255, 0.45);
  box-shadow: 0 0 0 1px rgba(55, 148, 255, 0.2);
}

textarea {
  width: 100%;
  resize: none;
  border: none;
  background: transparent;
  color: var(--text-primary);
  min-height: 44px;
  max-height: 160px;
  padding: 10px 12px 4px;
  line-height: 1.5;
  font-size: 13px;
}

textarea::placeholder {
  color: var(--text-muted);
}

textarea:focus {
  outline: none;
}

.composer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 8px 8px;
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.mode-pill {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-soft);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.model-picker {
  position: relative;
}

.model-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-secondary);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-btn:hover:not(:disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.chev {
  font-size: 9px;
  opacity: 0.7;
}

.model-menu {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 0;
  min-width: 180px;
  margin: 0;
  padding: 4px;
  list-style: none;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
  z-index: 30;
}

.model-menu li {
  padding: 6px 8px;
  font-size: 12px;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.model-menu li:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.model-menu li.active {
  color: var(--accent);
}

.bar-right {
  display: flex;
  gap: 4px;
}

.action {
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  display: grid;
  place-items: center;
  font-size: 12px;
}

.action.send {
  background: var(--accent);
  color: #fff;
}

.action.send:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.action.stop {
  background: var(--bg-hover);
  color: var(--text-primary);
}

@keyframes pulse {
  50% {
    opacity: 0.35;
  }
}
</style>
