package com.example.agent.core.memory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMessageDTO {

    /**
     * USER / ASSISTANT / SYSTEM / TOOL
     */
    private String type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * tool 调用名称
     * queryThemeStatus
     */
    private String toolName;

    /**
     * tool 调用参数
     */
    private String toolArguments;

    /**
     * tool 返回结果
     */
    private String toolResponse;

    /**
     * reasoning/thinking
     * 深度思考内容
     */
    private String reasoningContent;

    /**
     * token usage
     */
    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 扩展 metadata
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}