package com.example.agent.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentRouter 的结构化路由结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRouteResultVO {

    /**
     * 目标 Agent 编码：OPERATION_QA、TICKET、OPS、AGENT_ROUTER。
     */
    private String agentCode;

    /**
     * 目标 Agent 名称。
     */
    private String agentName;

    /**
     * 当前对话链路 ID，用于查询 Agent Trace。
     */
    private String traceId;

    /**
     * 路由原因，便于前端调试和后端排查。
     */
    private String reason;

    /**
     * 路由置信度，取值 0 到 1。
     */
    private Double confidence;

    /**
     * 当前问题是否需要先向用户澄清。
     */
    private Boolean needClarify;

    /**
     * 需要澄清时返回给用户的问题。
     */
    private String clarifyQuestion;
}
