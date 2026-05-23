package com.example.agent.core.agent.impl;

import com.example.agent.core.agent.AbstractAgent;
import com.example.agent.core.agent.AgentCode;
import com.example.agent.core.domain.vo.ChatEventVO;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 工单 Agent 占位实现。
 */
@Component
public class TicketAgent extends AbstractAgent {

    @Override
    public AgentCode agentCode() {
        return AgentCode.TICKET;
    }

    @Override
    public String agentName() {
        return AgentCode.TICKET.getName();
    }

    @Override
    public Flux<ChatEventVO> chat(String userMessage, String chatId, String userId) {
        return Flux.just(
                dataEvent("工单 Agent 框架已接入。当前版本暂未展开工单分类、优先级判断和开发组分派能力，建议先提交工单并补充问题背景、影响范围和复现步骤。"),
                ChatEventVO.stop()
        );
    }
}
