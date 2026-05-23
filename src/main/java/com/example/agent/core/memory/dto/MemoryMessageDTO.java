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
     * 消息类型：USER / ASSISTANT / SYSTEM / TOOL。
     */
    private String type;

    /**
     * 消息内容。
     */
    private String content;

    /**
     * Tool 调用名称，例如 queryThemeBusinessSnapshot。
     */
    private String toolName;

    /**
     * Tool 调用参数。
     */
    private String toolArguments;

    /**
     * Tool 返回结果。
     */
    private String toolResponse;

    /**
     * 模型推理内容，部分模型会返回 reasoning/thinking 字段。
     */
    private String reasoningContent;

    /**
     * prompt token 数量。
     */
    private Integer promptTokens;

    /**
     * completion token 数量。
     */
    private Integer completionTokens;

    /**
     * 总 token 数量。
     */
    private Integer totalTokens;

    /**
     * 模型名称。
     */
    private String model;

    /**
     * Spring AI 消息元数据。
     */
    private Map<String, Object> metadata;

    /**
     * 写入记忆的时间。
     */
    private LocalDateTime createTime;
}
