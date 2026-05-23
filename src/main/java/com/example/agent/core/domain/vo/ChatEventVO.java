package com.example.agent.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat event payload returned through SSE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventVO {

    public static final String EVENT_TYPE_DATA = "001";
    public static final String EVENT_TYPE_STOP = "002";
    public static final String EVENT_TYPE_ERROR = "003";

    /**
     * Event payload.
     */
    private Object eventData;

    /**
     * Event type: 001-data, 002-stop, 003-error.
     */
    private String eventType;

    public static ChatEventVO data(Object eventData) {
        return ChatEventVO.builder()
                .eventType(EVENT_TYPE_DATA)
                .eventData(eventData)
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
}



