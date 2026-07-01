<script setup lang="ts">
import { ref, watch } from "vue";
import { useChatStore } from "../stores/chat";
import { useSettingsStore } from "../stores/settings";

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ close: [] }>();

const settings = useSettingsStore();
const chat = useChatStore();

const baseUrl = ref(settings.settings.serviceBaseUrl);
const saving = ref(false);
const saveError = ref<string | null>(null);

watch(
  () => props.open,
  (open) => {
    if (open) {
      baseUrl.value = settings.settings.serviceBaseUrl;
      saveError.value = null;
    }
  },
);

async function save() {
  saveError.value = null;
  saving.value = true;
  try {
    settings.save({ serviceBaseUrl: baseUrl.value.trim() });
    await chat.loadModels();
    emit("close");
  } catch (e) {
    saveError.value = e instanceof Error ? e.message : "保存失败";
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="overlay" @click.self="emit('close')">
      <aside class="panel" role="dialog" aria-label="设置">
        <header class="panel-header">
          <h2>设置</h2>
          <button type="button" class="icon-btn" aria-label="关闭" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </header>

        <div class="panel-body">
          <label class="field">
            <span class="label">服务地址</span>
            <input v-model="baseUrl" type="url" placeholder="http://127.0.0.1:8010" />
            <span class="hint">连接本地或远程 Aegis 后端</span>
          </label>
          <p v-if="saveError" class="notice error">{{ saveError }}</p>
        </div>

        <footer class="panel-footer">
          <button type="button" class="btn ghost" @click="emit('close')">取消</button>
          <button type="button" class="btn primary" :disabled="saving" @click="save">
            {{ saving ? "保存中…" : "保存" }}
          </button>
        </footer>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  justify-content: flex-end;
  animation: fade-in 0.15s ease;
}

.panel {
  width: min(24rem, 100vw);
  height: 100%;
  background: var(--bg-elevated);
  border-left: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  animation: slide-in 0.2s ease;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.1rem 1.25rem;
  border-bottom: 1px solid var(--border-subtle);
}

.panel-header h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
}

.icon-btn {
  color: var(--text-secondary);
  padding: 0.35rem;
  border-radius: var(--radius-sm);
}

.icon-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.label {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.field input {
  background: var(--bg-surface);
  color: var(--text-primary);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: 0.6rem 0.75rem;
}

.hint {
  font-size: 0.75rem;
  color: var(--text-muted);
  line-height: 1.4;
}

.notice.error {
  margin-top: 0.75rem;
  font-size: 0.8rem;
  padding: 0.65rem 0.75rem;
  border-radius: var(--radius-sm);
  background: var(--danger-bg);
  color: var(--danger-text);
}

.panel-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--border-subtle);
}

.btn {
  border-radius: var(--radius-full);
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.btn.ghost {
  color: var(--text-secondary);
}

.btn.ghost:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.btn.primary {
  background: var(--accent);
  color: var(--accent-fg);
}

.btn.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
}

@keyframes slide-in {
  from {
    transform: translateX(100%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .overlay,
  .panel {
    animation: none;
  }
}
</style>
