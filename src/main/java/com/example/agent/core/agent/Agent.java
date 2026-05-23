package com.example.agent.core.agent;

import com.example.agent.core.domain.vo.ChatEventVO;
import reactor.core.publisher.Flux;

/**
 * 业务 Agent 的统一接口。
 *
 * <p>所有真正处理用户问题的 Agent 都实现该接口，入口层只需要通过 AgentCode 找到目标 Agent，
 * 不关心各 Agent 内部使用 RAG、Tool 还是其他业务系统。</p>
 */
public interface Agent {

    /**
     * Agent 编码，用于路由和注册。
     */
    AgentCode agentCode();

    /**
     * Agent 中文名称，用于日志和前端展示。
     */
    String agentName();

    /**
     * 处理用户问题并通过 SSE 事件流返回结果。
     */
    Flux<ChatEventVO> chat(String userMessage, String chatId, String userId, String traceId);
}
