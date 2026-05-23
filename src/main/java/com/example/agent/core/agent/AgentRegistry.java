package com.example.agent.core.agent;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Agent 注册表。
 *
 * <p>Spring 启动时收集所有业务 Agent，路由结果只需要携带 AgentCode，
 * ChatService 就可以通过注册表找到具体执行者。</p>
 */
@Component
public class AgentRegistry {

    private final Map<AgentCode, Agent> agentMap = new EnumMap<>(AgentCode.class);

    public AgentRegistry(List<Agent> agents) {
        for (Agent agent : agents) {
            agentMap.put(agent.agentCode(), agent);
        }
    }

    public Optional<Agent> getAgent(AgentCode agentCode) {
        return Optional.ofNullable(agentMap.get(agentCode));
    }
}
