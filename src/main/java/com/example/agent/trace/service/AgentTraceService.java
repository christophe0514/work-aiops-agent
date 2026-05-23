package com.example.agent.trace.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.agent.trace.domain.entity.AgentTraceLog;
import com.example.agent.trace.domain.vo.AgentTraceLogVO;

import java.util.List;

public interface AgentTraceService extends IService<AgentTraceLog> {

    String createTrace(String userMessage, String chatId, String userId);

    void record(String traceId, String agentCode, String agentName, String stage, String eventType, String status, Object eventData);

    void recordError(String traceId, String agentCode, String agentName, String stage, String errorMessage, Throwable throwable);

    void recordToolCall(String traceId, String agentCode, String agentName, String toolName, Object arguments);

    void recordToolResult(String traceId, String agentCode, String agentName, String toolName, Object result, long durationMs);

    List<AgentTraceLogVO> listByTraceId(String traceId);
}
