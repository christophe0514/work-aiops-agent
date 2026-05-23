package com.example.agent.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.agent.trace.domain.entity.AgentTraceLog;
import com.example.agent.trace.domain.vo.AgentTraceLogVO;
import com.example.agent.trace.mapper.AgentTraceLogMapper;
import com.example.agent.trace.service.AgentTraceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AgentTraceServiceImpl extends ServiceImpl<AgentTraceLogMapper, AgentTraceLog> implements AgentTraceService {

    private static final int MAX_TEXT_LENGTH = 8000;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String createTrace(String userMessage, String chatId, String userId) {
        String traceId = "trace-" + UUID.randomUUID();
        AgentTraceLog log = baseLog(traceId, null, null, "chat", "trace_start", "success");
        log.setUserMessage(limit(userMessage));
        log.setUserId(defaultIfBlank(userId, "anonymous"));
        log.setChatId(chatId);
        log.setConversationId(defaultIfBlank(userId, "anonymous") + "_" + defaultIfBlank(chatId, traceId));
        log.setEventData(toJsonString(new TraceStart(userMessage, chatId, userId)));
        saveQuietly(log);
        return traceId;
    }

    @Override
    public void record(String traceId, String agentCode, String agentName, String stage, String eventType, String status, Object eventData) {
        if (!StringUtils.hasText(traceId)) {
            return;
        }
        AgentTraceLog log = baseLog(traceId, agentCode, agentName, stage, eventType, status);
        log.setEventData(toJsonString(eventData));
        saveQuietly(log);
    }

    @Override
    public void recordError(String traceId, String agentCode, String agentName, String stage, String errorMessage, Throwable throwable) {
        if (!StringUtils.hasText(traceId)) {
            return;
        }
        AgentTraceLog log = baseLog(traceId, agentCode, agentName, stage, "error", "failed");
        log.setErrorMessage(limit(errorMessage));
        log.setEventData(toJsonString(new ErrorSnapshot(errorMessage, throwable == null ? null : throwable.getClass().getSimpleName())));
        saveQuietly(log);
    }

    @Override
    public void recordToolCall(String traceId, String agentCode, String agentName, String toolName, Object arguments) {
        record(traceId, agentCode, agentName, "tool", toolName + "_call", "running", arguments);
    }

    @Override
    public void recordToolResult(String traceId, String agentCode, String agentName, String toolName, Object result, long durationMs) {
        if (!StringUtils.hasText(traceId)) {
            return;
        }
        AgentTraceLog log = baseLog(traceId, agentCode, agentName, "tool", toolName + "_result", "success");
        log.setDurationMs(durationMs);
        log.setEventData(toJsonString(result));
        saveQuietly(log);
    }

    @Override
    public List<AgentTraceLogVO> listByTraceId(String traceId) {
        return lambdaQuery()
                .eq(AgentTraceLog::getTraceId, traceId)
                .orderByAsc(AgentTraceLog::getId)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    private AgentTraceLog baseLog(String traceId, String agentCode, String agentName, String stage, String eventType, String status) {
        AgentTraceLog log = new AgentTraceLog();
        log.setTraceId(traceId);
        log.setAgentCode(agentCode);
        log.setAgentName(agentName);
        log.setStage(stage);
        log.setEventType(eventType);
        log.setStatus(status);
        log.setCreatedTime(LocalDateTime.now());

        AgentTraceLog start = findStartQuietly(traceId);
        if (start != null) {
            log.setConversationId(start.getConversationId());
            log.setUserId(start.getUserId());
            log.setChatId(start.getChatId());
            log.setUserMessage(start.getUserMessage());
        }
        return log;
    }

    private AgentTraceLog findStartQuietly(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return null;
        }
        try {
            return getOne(new LambdaQueryWrapper<AgentTraceLog>()
                    .eq(AgentTraceLog::getTraceId, traceId)
                    .eq(AgentTraceLog::getEventType, "trace_start")
                    .last("limit 1"), false);
        } catch (Exception ignored) {
            return null;
        }
    }

    private AgentTraceLogVO toVO(AgentTraceLog log) {
        return AgentTraceLogVO.builder()
                .id(log.getId())
                .traceId(log.getTraceId())
                .conversationId(log.getConversationId())
                .userId(log.getUserId())
                .chatId(log.getChatId())
                .userMessage(log.getUserMessage())
                .agentCode(log.getAgentCode())
                .agentName(log.getAgentName())
                .stage(log.getStage())
                .eventType(log.getEventType())
                .status(log.getStatus())
                .eventData(log.getEventData())
                .errorMessage(log.getErrorMessage())
                .durationMs(log.getDurationMs())
                .promptTokens(log.getPromptTokens())
                .completionTokens(log.getCompletionTokens())
                .totalTokens(log.getTotalTokens())
                .createdTime(log.getCreatedTime())
                .build();
    }

    private void saveQuietly(AgentTraceLog log) {
        try {
            save(log);
        } catch (Exception ignored) {
            // Trace 不应影响主对话链路；表未初始化或数据库异常时降级为仅业务返回。
        }
    }

    private String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return limit(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException ex) {
            return limit(String.valueOf(value));
        }
    }

    private String limit(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private record TraceStart(String userMessage, String chatId, String userId) {
    }

    private record ErrorSnapshot(String message, String exceptionType) {
    }
}
