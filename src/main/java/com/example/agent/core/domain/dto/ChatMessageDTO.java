package com.example.agent.core.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {

    private String userMessage;

    /**
     * 用户 ID，用于隔离不同用户的会话记忆。
     */
    private String userId;

    /**
     * 会话 ID，同一用户下不同会话互不影响。
     */
    private String chatId;
}
