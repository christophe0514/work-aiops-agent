package com.example.agent.core.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private String userMessage;

    // 用户ID
    private String userId;

    // 聊天线程ID
    private String chatId;
}
