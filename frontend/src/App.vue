<script setup lang="ts">
import AgentSidebar from "@/components/AgentSidebar.vue";
import ChatComposer from "@/components/ChatComposer.vue";
import ChatHeader from "@/components/ChatHeader.vue";
import KnowledgeBasePanel from "@/components/KnowledgeBasePanel.vue";
import MessageList from "@/components/MessageList.vue";
import QuickPrompts from "@/components/QuickPrompts.vue";
import { useChatSession } from "@/composables/useChatSession";
import { agents, quickPrompts } from "@/config/agents";
import { ref } from "vue";

const session = useChatSession();
const activeView = ref<"chat" | "kb">("chat");
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
      <div class="view-tabs">
        <button :class="{ active: activeView === 'chat' }" type="button" @click="activeView = 'chat'">
          Agent 对话
        </button>
        <button :class="{ active: activeView === 'kb' }" type="button" @click="activeView = 'kb'">
          知识库管理
        </button>
      </div>

      <template v-if="activeView === 'chat'">
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
      </template>

      <KnowledgeBasePanel v-else />
    </section>
  </main>
</template>
