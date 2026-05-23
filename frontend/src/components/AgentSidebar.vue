<script setup lang="ts">
import type { AgentProfile } from "@/types/chat";

defineProps<{
  agents: AgentProfile[];
  elapsedMs: number;
  loading: boolean;
  userId: string;
  chatId: string;
  conversationId: string;
}>();

const emit = defineEmits<{
  "update:userId": [value: string];
  "update:chatId": [value: string];
  newChat: [];
}>();

function formatElapsed(value: number) {
  if (!value) {
    return "--";
  }

  if (value < 1000) {
    return `${value} ms`;
  }

  return `${(value / 1000).toFixed(1)} s`;
}
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">AI</div>
      <div>
        <h1>AIOps Agent</h1>
        <p>主题平台联调台</p>
      </div>
    </div>

    <section class="panel">
      <div class="panel-title">Agent</div>
      <div
        v-for="agent in agents"
        :key="agent.id"
        class="agent-row"
        :class="{ active: agent.status === 'active', disabled: agent.status !== 'active' }"
      >
        <span class="status-dot" :class="{ muted: agent.status !== 'active' }" />
        <div>
          <strong>{{ agent.name }}</strong>
          <small>{{ agent.code }}</small>
        </div>
      </div>
    </section>

    <section class="panel session-panel">
      <div class="panel-title">会话记忆</div>
      <label class="field">
        <span>用户 ID</span>
        <input
          :value="userId"
          :disabled="loading"
          @change="emit('update:userId', ($event.target as HTMLInputElement).value)"
        />
      </label>
      <label class="field">
        <span>会话 ID</span>
        <input
          :value="chatId"
          :disabled="loading"
          @change="emit('update:chatId', ($event.target as HTMLInputElement).value)"
        />
      </label>
      <button class="small-btn" type="button" :disabled="loading" @click="emit('newChat')">
        新建会话
      </button>
      <p class="hint">后端使用 userId_chatId 写入 Redis 会话记忆。</p>
    </section>

    <section class="panel">
      <div class="panel-title">接口状态</div>
      <dl class="meta">
        <div><dt>后端</dt><dd>localhost:18080</dd></div>
        <div><dt>代理</dt><dd>/api/chat</dd></div>
        <div><dt>模式</dt><dd>text/event-stream</dd></div>
        <div><dt>状态</dt><dd>{{ loading ? "响应中" : "就绪" }}</dd></div>
        <div><dt>耗时</dt><dd>{{ formatElapsed(elapsedMs) }}</dd></div>
        <div><dt>记忆键</dt><dd>{{ conversationId }}</dd></div>
      </dl>
    </section>
  </aside>
</template>
