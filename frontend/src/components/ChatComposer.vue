<script setup lang="ts">
const model = defineModel<string>({ required: true });

defineProps<{
  disabled: boolean;
}>();

const emit = defineEmits<{
  send: [];
  clear: [];
}>();

function handleKeydown(event: KeyboardEvent) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    emit("send");
  }
}
</script>

<template>
  <form class="composer" @submit.prevent="emit('send')">
    <textarea
      v-model="model"
      placeholder="输入要调试的问题，例如：刚才那个主题为什么前台看不到？"
      :disabled="disabled"
      @keydown="handleKeydown"
    />
    <div class="composer-actions">
      <button class="ghost-btn" type="button" :disabled="disabled" @click="emit('clear')">
        清空
      </button>
      <button class="send-btn" type="submit" :disabled="disabled">
        发送
      </button>
    </div>
  </form>
</template>
