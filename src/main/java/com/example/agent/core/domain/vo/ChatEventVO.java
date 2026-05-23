package com.example.agent.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 返回给前端的统一事件体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventVO {

    public static final String EVENT_TYPE_DATA = "001";
    public static final String EVENT_TYPE_STOP = "002";
    public static final String EVENT_TYPE_ERROR = "003";
    public static final String EVENT_TYPE_ROUTE = "004";

    /**
     * 事件数据。普通回答为文本，路由事件为路由结果对象，错误事件为错误说明。
     */
    private Object eventData;

    /**
     * 事件类型：001-回答内容，002-结束，003-错误，004-路由信息。
     */
    private String eventType;

    /**
     * 当前事件归属的 Agent 编码，便于前端展示回答来源。
     */
    private String agentCode;

    /**
     * 当前事件归属的 Agent 名称，便于前端展示回答来源。
     */
    private String agentName;

    public static ChatEventVO data(Object eventData) {
        return ChatEventVO.builder()
                .eventType(EVENT_TYPE_DATA)
                .eventData(eventData)
                .build();
    }

    public static ChatEventVO data(Object eventData, String agentCode, String agentName) {
        return ChatEventVO.builder()
                .eventType(EVENT_TYPE_DATA)
                .eventData(eventData)
                .agentCode(agentCode)
                .agentName(agentName)
                .build();
    }

    public static ChatEventVO route(Object eventData, String agentCode, String agentName) {
        return ChatEventVO.builder()
                .eventType(EVENT_TYPE_ROUTE)
                .eventData(eventData)
                .agentCode(agentCode)
                .agentName(agentName)
                .build();
    }

    public static ChatEventVO stop() {
        return ChatEventVO.builder()
                .eventType(EVENT_TYPE_STOP)
                .eventData("[DONE]")
                .build();
    }

    public static ChatEventVO error(Object eventData) {
        return ChatEventVO.builder()
                .eventType(EVENT_TYPE_ERROR)
                .eventData(eventData)
                .build();
    }

    public static ChatEventVO error(Object eventData, String agentCode, String agentName) {
        return ChatEventVO.builder()
                .eventType(EVENT_TYPE_ERROR)
                .eventData(eventData)
                .agentCode(agentCode)
                .agentName(agentName)
                .build();
    }
}



