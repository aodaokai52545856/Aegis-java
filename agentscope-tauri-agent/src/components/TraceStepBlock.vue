<script setup lang="ts">
import type { TraceStep } from "../types/chat";
import { computed, ref, watch } from "vue";

const props = defineProps<{
  step: TraceStep;
}>();

const expanded = ref(props.step.status === "running");

watch(
  () => props.step.status,
  (status) => {
    if (status === "running") expanded.value = true;
    if (status === "done" && props.step.type === "thinking") {
      expanded.value = false;
    }
  },
);

const label = computed(() => {
  if (props.step.type === "thinking") {
    return props.step.status === "running" ? "Thinking…" : "Thought";
  }
  if (props.step.type === "tool_call") {
    return props.step.status === "running"
      ? `Calling ${props.step.tool}`
      : `Called ${props.step.tool}`;
  }
  return `Result · ${props.step.tool}`;
});

const preview = computed(() => {
  const text = props.step.content?.trim() ?? "";
  if (!text) return "";
  const line = text.split("\n")[0];
  return line.length > 72 ? `${line.slice(0, 72)}…` : line;
});

const icon = computed(() => {
  if (props.step.type === "thinking") return "◌";
  return "⬢";
});
</script>

<template>
  <div class="trace-block" :class="[step.type, step.status, { expanded }]">
    <button type="button" class="trace-toggle" @click="expanded = !expanded">
      <span class="chevron" aria-hidden="true">{{ expanded ? "▾" : "▸" }}</span>
      <span class="icon" aria-hidden="true">{{ icon }}</span>
      <span class="label">{{ label }}</span>
      <span v-if="!expanded && preview" class="preview">{{ preview }}</span>
      <span v-if="step.status === 'running'" class="pulse" aria-label="进行中" />
    </button>
    <div v-if="expanded && step.content" class="trace-body">
      <pre>{{ step.content }}</pre>
    </div>
  </div>
</template>

<style scoped>
.trace-block {
  border-left: 2px solid transparent;
  padding-left: 2px;
}

.trace-block.thinking {
  border-left-color: rgba(139, 139, 139, 0.45);
}

.trace-block.tool_call {
  border-left-color: rgba(78, 201, 176, 0.45);
}

.trace-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 4px 6px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-align: left;
  font-size: 12px;
}

.trace-toggle:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.chevron {
  width: 10px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.icon {
  width: 14px;
  text-align: center;
  flex-shrink: 0;
  color: var(--thought);
  font-size: 11px;
}

.tool_call .icon {
  color: var(--tool);
}

.label {
  font-weight: 500;
  flex-shrink: 0;
}

.preview {
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  margin-left: auto;
  flex-shrink: 0;
  animation: pulse 1.2s ease-in-out infinite;
}

.trace-body {
  margin: 2px 0 6px 22px;
  padding: 8px 10px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.trace-body pre {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 11px;
  line-height: 1.5;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.thinking .trace-body pre {
  font-family: var(--font-ui);
  font-size: 12px;
}

@keyframes pulse {
  50% {
    opacity: 0.35;
  }
}
</style>
