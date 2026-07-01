<script setup lang="ts">
import { isTauri } from "@tauri-apps/api/core";
import { getCurrentWindow } from "@tauri-apps/api/window";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useChatStore } from "../stores/chat";
import AppMark from "./AppMark.vue";

const emit = defineEmits<{ openSettings: [] }>();

const chat = useChatStore();
const inTauri = isTauri();

type MenuId = "agent" | "settings";

const openMenu = ref<MenuId | null>(null);
const barRef = ref<HTMLElement | null>(null);
const isMaximized = ref(false);

const sessionLabel = computed(() => chat.sessionId.slice(0, 8));

let unlistenResize: (() => void) | null = null;

function toggleMenu(id: MenuId) {
  openMenu.value = openMenu.value === id ? null : id;
}

function closeMenu() {
  openMenu.value = null;
}

function onDocClick(e: MouseEvent) {
  if (!barRef.value?.contains(e.target as Node)) closeMenu();
}

function onDocKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") closeMenu();
}

function pickModel(id: string) {
  chat.selectedModel = id;
  closeMenu();
}

function newSession() {
  chat.resetSession();
  closeMenu();
}

function run(action: () => void) {
  action();
  closeMenu();
}

async function syncMaximized() {
  if (!inTauri) return;
  isMaximized.value = await getCurrentWindow().isMaximized();
}

async function toggleMaximize() {
  if (!inTauri) return;
  await getCurrentWindow().toggleMaximize();
  await syncMaximized();
}

async function minimizeWindow() {
  if (!inTauri) return;
  await getCurrentWindow().minimize();
}

async function closeWindow() {
  if (!inTauri) return;
  await getCurrentWindow().close();
}

async function onTitlebarDblClick() {
  if (!inTauri) return;
  await getCurrentWindow().toggleMaximize();
  await syncMaximized();
}

onMounted(async () => {
  document.addEventListener("click", onDocClick);
  document.addEventListener("keydown", onDocKeydown);
  if (inTauri) {
    await syncMaximized();
    unlistenResize = await getCurrentWindow().onResized(() => {
      void syncMaximized();
    });
  }
});

onUnmounted(() => {
  document.removeEventListener("click", onDocClick);
  document.removeEventListener("keydown", onDocKeydown);
  unlistenResize?.();
});
</script>

<template>
  <nav ref="barRef" class="app-menubar" aria-label="应用菜单">
    <div class="menu-brand">
      <AppMark />
      <span class="panel-title">Agent</span>
      <div class="menu-root">
        <div class="menu-slot">
          <button
            type="button"
            class="menu-trigger"
            :class="{ open: openMenu === 'agent' }"
            @click.stop="toggleMenu('agent')"
          >
            会话
          </button>
          <div v-if="openMenu === 'agent'" class="menu-dropdown" role="menu" @click.stop>
            <p class="menu-section-label">模型</p>
            <button
              v-for="model in chat.models"
              :key="model.id"
              type="button"
              class="menu-item"
              :class="{ active: chat.selectedModel === model.id }"
              :disabled="chat.loading"
              @click="pickModel(model.id)"
            >
              <span class="menu-check">{{ chat.selectedModel === model.id ? "✓" : "" }}</span>
              <span class="menu-item-label">{{ model.name }}</span>
            </button>
            <p v-if="chat.models.length === 0" class="menu-empty">暂无可用模型</p>
            <div class="menu-sep" />
            <p class="menu-meta">Session {{ sessionLabel }}</p>
            <button type="button" class="menu-item" :disabled="chat.loading" @click="newSession">
              新会话
            </button>
          </div>
        </div>

        <div class="menu-slot">
          <button
            type="button"
            class="menu-trigger"
            :class="{ open: openMenu === 'settings' }"
            @click.stop="toggleMenu('settings')"
          >
            设置
          </button>
          <div v-if="openMenu === 'settings'" class="menu-dropdown" role="menu" @click.stop>
            <button type="button" class="menu-item" @click="run(() => emit('openSettings'))">
              服务地址…
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="titlebar-drag" data-tauri-drag-region @dblclick="onTitlebarDblClick" />

    <div v-if="inTauri" class="titlebar-controls">
      <button type="button" class="win-btn" aria-label="最小化" @click="minimizeWindow">
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path d="M3.5 8h9" stroke="currentColor" stroke-width="1.25" stroke-linecap="round" />
        </svg>
      </button>
      <button type="button" class="win-btn" :aria-label="isMaximized ? '向下还原' : '最大化'" @click="toggleMaximize">
        <svg v-if="isMaximized" viewBox="0 0 16 16" aria-hidden="true">
          <rect x="3.25" y="5.75" width="8.5" height="8.5" rx="0.75" fill="none" stroke="currentColor" stroke-width="1.25" />
        </svg>
        <svg v-else viewBox="0 0 16 16" aria-hidden="true">
          <rect x="3.25" y="3.25" width="9.5" height="9.5" rx="0.75" fill="none" stroke="currentColor" stroke-width="1.25" />
        </svg>
      </button>
      <button type="button" class="win-btn win-btn-close" aria-label="关闭" @click="closeWindow">
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <path d="M4 4l8 8M12 4L4 12" stroke="currentColor" stroke-width="1.25" stroke-linecap="round" />
        </svg>
      </button>
    </div>
  </nav>
</template>

<style scoped>
.app-menubar {
  display: flex;
  align-items: stretch;
  flex-shrink: 0;
  min-height: 32px;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--header-bg);
  position: relative;
  z-index: 20;
}

.menu-brand {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 6px;
  padding-left: 8px;
}

.panel-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  margin-right: 4px;
}

.menu-root {
  display: flex;
  align-items: stretch;
}

.titlebar-drag {
  flex: 1;
  min-width: 48px;
}

.titlebar-controls {
  display: flex;
  flex-shrink: 0;
}

.win-btn {
  display: grid;
  place-items: center;
  width: 46px;
  color: var(--text-secondary);
}

.win-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.win-btn-close:hover {
  background: #c42b1c;
  color: #fff;
}

.win-btn svg {
  width: 14px;
  height: 14px;
}

.menu-slot {
  position: relative;
}

.menu-trigger {
  min-height: 32px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
}

.menu-trigger:hover,
.menu-trigger.open {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.menu-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  min-width: 200px;
  padding: 4px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  z-index: 100;
}

.menu-section-label,
.menu-meta {
  margin: 4px 8px 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.menu-meta {
  text-transform: none;
  font-weight: 500;
  letter-spacing: 0;
}

.menu-empty {
  margin: 0 8px 6px;
  font-size: 11px;
  color: var(--text-muted);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 6px 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-secondary);
  text-align: left;
}

.menu-item:hover:not(:disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.menu-item.active {
  color: var(--accent);
}

.menu-item:disabled {
  opacity: 0.4;
}

.menu-check {
  width: 12px;
  font-size: 11px;
}

.menu-item-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-sep {
  height: 1px;
  margin: 4px 6px;
  background: var(--border-subtle);
}
</style>
