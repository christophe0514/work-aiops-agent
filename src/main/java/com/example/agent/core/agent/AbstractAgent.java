package com.example.agent.core.agent;

import com.example.agent.core.domain.vo.ChatEventVO;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.UUID;

/**
 * 业务 Agent 公共基类。
 */
public abstract class AbstractAgent implements Agent {

    /**
     * 统一生成会话记忆 ID，避免不同用户或不同会话之间串上下文。
     */
    protected String normalizeConversationId(String userId, String chatId) {
        String normalizedUserId = hasText(userId) ? userId.trim() : "anonymous";
        String normalizedChatId = hasText(chatId) ? chatId.trim() : UUID.randomUUID().toString();
        return normalizedUserId + "_" + normalizedChatId;
    }

    protected boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    protected String conversationIdParam() {
        return ChatMemory.CONVERSATION_ID;
    }

    protected ChatEventVO dataEvent(Object eventData) {
        return ChatEventVO.data(eventData, agentCode().getCode(), agentName());
    }

    protected ChatEventVO errorEvent(Object eventData) {
        return ChatEventVO.error(eventData, agentCode().getCode(), agentName());
    }

    protected Flux<ChatEventVO> appendStop(Flux<ChatEventVO> contentStream) {
        return Flux.concat(contentStream.filter(Objects::nonNull), Flux.just(ChatEventVO.stop()));
    }
}
