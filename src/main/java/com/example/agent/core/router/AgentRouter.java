package com.example.agent.core.router;

import com.example.agent.core.domain.vo.AgentRouteResultVO;

/**
 * 入口路由 Agent。
 *
 * <p>该组件只负责识别用户问题意图并选择目标业务 Agent，不直接回答业务问题。</p>
 */
public interface AgentRouter {

    /**
     * 使用大模型分析用户问题，返回结构化路由结果。
     */
    AgentRouteResultVO route(String userMessage);
}
