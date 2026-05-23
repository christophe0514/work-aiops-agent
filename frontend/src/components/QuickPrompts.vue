<script setup lang="ts">
defineProps<{
  items: string[];
  disabled: boolean;
}>();

const emit = defineEmits<{
  select: [value: string];
}>();
</script>

<template>
  <div class="quick-bar">
    <button
      v-for="item in items"
      :key="item"
      class="quick-btn"
      type="button"
      :disabled="disabled"
      @click="emit('select', item)"
    >
      {{ item }}
    </button>
  </div>
</template>
