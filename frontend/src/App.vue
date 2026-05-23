<script setup lang="ts">
import AgentSidebar from "@/components/AgentSidebar.vue";
import ChatComposer from "@/components/ChatComposer.vue";
import ChatHeader from "@/components/ChatHeader.vue";
import MessageList from "@/components/MessageList.vue";
import QuickPrompts from "@/components/QuickPrompts.vue";
import { useChatSession } from "@/composables/useChatSession";
import { agents, quickPrompts } from "@/config/agents";

const session = useChatSession();
</script>

<template>
  <main class="app-shell">
    <AgentSidebar
      :agents="agents"
      :elapsed-ms="session.elapsedMs.value"
      :loading="session.loading.value"
      :user-id="session.userId.value"
      :chat-id="session.chatId.value"
      :conversation-id="session.conversationId.value"
      @update:user-id="session.setUserId"
      @update:chat-id="session.setChatId"
      @new-chat="session.newChat"
    />

    <section class="workspace">
      <ChatHeader
        :loading="session.loading.value"
        :conversation-id="session.conversationId.value"
      />

      <QuickPrompts
        :items="quickPrompts"
        :disabled="session.loading.value"
        @select="session.setDraft"
      />

      <MessageList :messages="session.messages.value" />

      <div v-if="session.error.value" class="error-box">
        {{ session.error.value }}
      </div>

      <ChatComposer
        v-model="session.draft.value"
        :disabled="session.loading.value"
        @clear="session.clear"
        @send="session.send"
      />
    </section>
  </main>
</template>
