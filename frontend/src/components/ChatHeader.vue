<script setup lang="ts">
defineProps<{
  loading: boolean;
  conversationId: string;
}>();
</script>

<template>
  <header class="topbar">
    <div>
      <h2>主题业务对话调试</h2>
      <p>验证 Prompt、SSE 结构化输出、Redis 会话记忆和后端对话链路</p>
    </div>
    <div class="topbar-actions">
      <div class="conversation-chip">{{ conversationId }}</div>
      <div class="run-state" :class="{ running: loading }">
        <span />
        {{ loading ? "响应中" : "就绪" }}
      </div>
    </div>
  </header>
</template>
