package com.example.agent.core.agent;

import java.util.Arrays;

/**
 * 系统内置 Agent 编码。
 */
public enum AgentCode {

    AGENT_ROUTER("AgentRouter", "路由 Agent"),
    OPERATION_QA("OperationQaAgent", "主题业务 Agent"),
    TICKET("TicketAgent", "工单 Agent"),
    OPS("OpsAgent", "运维排障 Agent");

    private final String code;
    private final String name;

    AgentCode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static AgentCode of(String value) {
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(value) || item.code.equalsIgnoreCase(value))
                .findFirst()
                .orElse(AGENT_ROUTER);
    }
}
