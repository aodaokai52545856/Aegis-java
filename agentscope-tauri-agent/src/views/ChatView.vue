<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from "vue";
import { useChatStore } from "../stores/chat";
import AppMenuBar from "../components/AppMenuBar.vue";
import ChatMessageList from "../components/ChatMessageList.vue";
import ChatInput from "../components/ChatInput.vue";
import SettingsPanel from "../components/SettingsPanel.vue";

const chat = useChatStore();
const showSettings = ref(false);
const listRef = ref<InstanceType<typeof ChatMessageList> | null>(null);
const inputRef = ref<InstanceType<typeof ChatInput> | null>(null);

const sessionShort = () => chat.sessionId.slice(0, 8);

onMounted(() => {
  chat.loadModels();
});

function onUseHint(text: string) {
  inputRef.value?.setInput(text);
  void nextTick(() => inputRef.value?.focus());
}

function newChat() {
  if (chat.loading) return;
  chat.resetSession();
}

watch(
  () => {
    const last = chat.messages[chat.messages.length - 1];
    return [chat.messages.length, chat.loading, last?.content, last?.trace?.length];
  },
  () => {
    void nextTick(() => listRef.value?.scrollToBottom());
  },
);
</script>

<template>
  <div class="shell">
    <AppMenuBar @open-settings="showSettings = true" />

    <div class="agent-toolbar">
      <button type="button" class="toolbar-btn" :disabled="chat.loading" @click="newChat">
        新会话
      </button>
      <span class="toolbar-sep" aria-hidden="true" />
      <span class="toolbar-meta">Session {{ sessionShort() }}</span>
      <span class="toolbar-meta model">{{ chat.selectedModelName }}</span>
    </div>

    <div class="page">
      <div v-if="chat.error" class="banner" role="alert">
        {{ chat.error }}
      </div>

      <main class="main">
        <ChatMessageList ref="listRef" @use-hint="onUseHint" />
        <ChatInput ref="inputRef" />
      </main>
    </div>

    <SettingsPanel :open="showSettings" @close="showSettings = false" />
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-base);
}

.agent-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding: 4px var(--panel-pad);
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-panel);
}

.toolbar-btn {
  font-size: 11px;
  color: var(--text-secondary);
  padding: 3px 8px;
  border-radius: var(--radius-sm);
}

.toolbar-btn:hover:not(:disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.toolbar-btn:disabled {
  opacity: 0.4;
}

.toolbar-sep {
  width: 1px;
  height: 14px;
  background: var(--border-subtle);
}

.toolbar-meta {
  font-size: 11px;
  color: var(--text-muted);
}

.toolbar-meta.model {
  margin-left: auto;
  color: var(--text-secondary);
}

.page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.banner {
  flex-shrink: 0;
  padding: 0.5rem 1rem;
  text-align: center;
  background: var(--danger-bg);
  color: var(--danger-text);
  font-size: 0.8rem;
  border-bottom: 1px solid rgba(239, 68, 68, 0.2);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
</style>
