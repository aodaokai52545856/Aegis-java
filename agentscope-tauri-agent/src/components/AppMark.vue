<script setup lang="ts">
</script>

<template>
  <span class="app-mark" aria-hidden="true">∞</span>
</template>

<style scoped>
.app-mark {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-sm);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 13px;
  font-weight: 700;
  user-select: none;
}
</style>
