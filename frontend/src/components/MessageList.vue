<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import type { ChatMessage } from "@/types/chat";

const props = defineProps<{
  messages: ChatMessage[];
}>();

const listRef = ref<HTMLElement | null>(null);

const roleName = {
  user: "用户",
  assistant: "Agent",
  system: "系统"
};

const orderedMessages = computed(() => props.messages);

watch(
  () => props.messages.map((message) => message.content).join(""),
  async () => {
    await nextTick();
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight;
    }
  }
);
</script>

<template>
  <section ref="listRef" class="conversation" aria-live="polite">
    <article
      v-for="message in orderedMessages"
      :key="message.id"
      class="message"
      :class="message.role"
    >
      <div class="avatar">{{ roleName[message.role].slice(0, 1) }}</div>
      <div class="bubble">
        <div class="message-head">
          <strong>{{ message.agentName || roleName[message.role] }}</strong>
          <span v-if="message.pending" class="typing">生成中</span>
        </div>
        <div class="message-body">
          {{ message.content || "等待流式内容..." }}
        </div>
      </div>
    </article>
  </section>
</template>
