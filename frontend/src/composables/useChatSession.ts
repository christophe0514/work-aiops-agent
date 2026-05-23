import { computed, nextTick, ref } from "vue";
import { streamChat } from "@/services/chatService";
import type { ChatMessage } from "@/types/chat";

const defaultUserId = "operator-demo";

function createChatId() {
  return `chat-${Date.now().toString(36)}`;
}

function createInitialMessage(chatId: string): ChatMessage {
  return {
    id: 1,
    role: "system",
    content: `当前调试目标：LLM AgentRouter 多智能体路由。当前会话 ID：${chatId}。同一 userId + chatId 会复用 Redis 中的对话记忆。`
  };
}

export function useChatSession() {
  const userId = ref(defaultUserId);
  const chatId = ref(createChatId());
  const messages = ref<ChatMessage[]>([createInitialMessage(chatId.value)]);
  const draft = ref("");
  const loading = ref(false);
  const error = ref("");
  const startedAt = ref(0);
  const now = ref(0);
  let messageSeq = 2;
  let timer: number | undefined;

  const conversationId = computed(() => `${userId.value}_${chatId.value}`);

  const elapsedMs = computed(() => {
    if (!startedAt.value) {
      return 0;
    }

    return Math.round((now.value || performance.now()) - startedAt.value);
  });

  function setDraft(value: string) {
    draft.value = value;
  }

  function setUserId(value: string) {
    userId.value = value.trim() || defaultUserId;
  }

  function setChatId(value: string) {
    chatId.value = value.trim() || createChatId();
    resetMessages();
  }

  function newChat() {
    chatId.value = createChatId();
    resetMessages();
  }

  function clear() {
    resetMessages();
    draft.value = "";
    error.value = "";
    startedAt.value = 0;
    now.value = 0;
  }

  async function send() {
    const content = draft.value.trim();

    if (!content || loading.value) {
      return;
    }

    error.value = "";
    loading.value = true;
    draft.value = "";
    startedAt.value = performance.now();
    now.value = startedAt.value;

    const assistantMessage: ChatMessage = {
      id: messageSeq++,
      role: "assistant",
      content: "",
      pending: true
    };

    messages.value.push({ id: messageSeq++, role: "user", content });
    messages.value.push(assistantMessage);
    const assistantId = assistantMessage.id;
    startTimer();

    try {
      await streamChat(
        {
          userMessage: content,
          userId: userId.value,
          chatId: chatId.value
        },
        async (delta) => {
          updateAssistantMessage(assistantId, (message) => {
            message.content += delta;
          });
          await nextTick();
        },
        (route) => {
          updateAssistantMessage(assistantId, (message) => {
            message.agentCode = route.agentCode;
            message.agentName = route.agentName;
          });
        }
      );
    } catch (err) {
      error.value = err instanceof Error ? err.message : "请求失败，请检查后端服务是否启动";
      updateAssistantMessage(assistantId, (message) => {
        message.content ||= "请求失败，暂未获得 Agent 回复。";
      });
    } finally {
      updateAssistantMessage(assistantId, (message) => {
        message.pending = false;
      });
      loading.value = false;
      stopTimer();
    }
  }

  function resetMessages() {
    messageSeq = 2;
    messages.value = [createInitialMessage(chatId.value)];
  }

  function updateAssistantMessage(id: number, updater: (message: ChatMessage) => void) {
    const target = messages.value.find((message) => message.id === id);

    if (target) {
      updater(target);
    }
  }

  function startTimer() {
    stopTimer();
    timer = window.setInterval(() => {
      now.value = performance.now();
    }, 200);
  }

  function stopTimer() {
    if (timer) {
      window.clearInterval(timer);
      timer = undefined;
    }

    if (startedAt.value) {
      now.value = performance.now();
    }
  }

  return {
    userId,
    chatId,
    conversationId,
    messages,
    draft,
    loading,
    error,
    elapsedMs,
    setDraft,
    setUserId,
    setChatId,
    newChat,
    clear,
    send
  };
}
