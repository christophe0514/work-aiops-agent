package com.example.agent.core.service.impl;

import com.example.agent.core.agent.Agent;
import com.example.agent.core.agent.AgentCode;
import com.example.agent.core.agent.AgentRegistry;
import com.example.agent.core.domain.vo.AgentRouteResultVO;
import com.example.agent.core.domain.vo.ChatEventVO;
import com.example.agent.core.router.AgentRouter;
import com.example.agent.core.service.ChatService;
import com.example.agent.trace.service.AgentTraceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * 对话入口服务。
 *
 * <p>该服务不再直接绑定某个业务 Agent，而是先请求 AgentRouter 做意图识别，
 * 再把用户问题分派给对应的业务 Agent。</p>
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private AgentRouter agentRouter;

    @Resource
    private AgentRegistry agentRegistry;

    @Resource
    private AgentTraceService agentTraceService;

    @Override
    public Flux<ChatEventVO> chat(String userMessage, String chatId, String userId) {
        if (!StringUtils.hasText(userMessage)) {
            return Flux.just(ChatEventVO.error("用户问题不能为空。"), ChatEventVO.stop());
        }

        String traceId = agentTraceService.createTrace(userMessage, chatId, userId);
        agentTraceService.record(traceId, AgentCode.AGENT_ROUTER.name(), AgentCode.AGENT_ROUTER.getName(),
                "router", "route_start", "running", userMessage);

        AgentRouteResultVO routeResult;
        try {
            routeResult = agentRouter.route(userMessage);
            agentTraceService.record(traceId, routeResult.getAgentCode(), routeResult.getAgentName(),
                    "router", "route_result", "success", routeResult);
        } catch (Exception ex) {
            agentTraceService.recordError(traceId, AgentCode.AGENT_ROUTER.name(), AgentCode.AGENT_ROUTER.getName(),
                    "router", "AgentRouter 路由失败", ex);
            return Flux.just(ChatEventVO.error("AgentRouter 路由失败，请稍后重试。"), ChatEventVO.stop());
        }

        AgentCode routedAgentCode = AgentCode.of(routeResult.getAgentCode());
        routeResult.setTraceId(traceId);
        ChatEventVO routeEvent = ChatEventVO.route(routeResult, routedAgentCode.getCode(), routeResult.getAgentName());

        if (Boolean.TRUE.equals(routeResult.getNeedClarify()) || AgentCode.AGENT_ROUTER.equals(routedAgentCode)) {
            String clarifyQuestion = StringUtils.hasText(routeResult.getClarifyQuestion())
                    ? routeResult.getClarifyQuestion()
                    : "请补充问题对象和场景，例如主题业务、工单处理或运维排障。";
            return Flux.just(routeEvent, ChatEventVO.data(clarifyQuestion, routedAgentCode.getCode(), routeResult.getAgentName()), ChatEventVO.stop());
        }

        return agentRegistry.getAgent(routedAgentCode)
                .map(agent -> Flux.concat(Flux.just(buildRouteEvent(routeResult, agent)), agent.chat(userMessage, chatId, userId, traceId)))
                .orElseGet(() -> Flux.just(
                        routeEvent,
                        ChatEventVO.error("未找到可处理该问题的业务 Agent，请检查 AgentRegistry 配置。"),
                        ChatEventVO.stop()
                ));
    }

    private ChatEventVO buildRouteEvent(AgentRouteResultVO routeResult, Agent agent) {
        routeResult.setAgentCode(agent.agentCode().name());
        routeResult.setAgentName(agent.agentName());
        return ChatEventVO.route(routeResult, agent.agentCode().getCode(), agent.agentName());
    }
}
