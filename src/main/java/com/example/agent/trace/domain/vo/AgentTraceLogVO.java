package com.example.agent.trace.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgentTraceLogVO {

    private Long id;

    private String traceId;

    private String conversationId;

    private String userId;

    private String chatId;

    private String userMessage;

    private String agentCode;

    private String agentName;

    private String stage;

    private String eventType;

    private String status;

    private String eventData;

    private String errorMessage;

    private Long durationMs;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private LocalDateTime createdTime;
}
