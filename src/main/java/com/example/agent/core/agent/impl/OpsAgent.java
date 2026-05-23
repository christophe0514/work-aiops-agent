package com.example.agent.core.agent.impl;

import com.example.agent.core.agent.AbstractAgent;
import com.example.agent.core.agent.AgentCode;
import com.example.agent.core.domain.vo.ChatEventVO;
import com.example.agent.trace.context.AgentTraceContext;
import com.example.agent.trace.service.AgentTraceService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * 运维排障 Agent。
 *
 * <p>负责接口异常、服务健康、告警、Trace、流水线等技术排障问题。
 * 当前底层 Tool 使用 Mock 数据，后续可以替换为真实监控、日志、Trace 和流水线平台。</p>
 */
@Component
public class OpsAgent extends AbstractAgent {

    @Resource
    @Qualifier("opsAgentClient")
    private ChatClient opsAgentClient;

    @Resource
    private AgentTraceService agentTraceService;

    @Override
    public AgentCode agentCode() {
        return AgentCode.OPS;
    }

    @Override
    public String agentName() {
        return AgentCode.OPS.getName();
    }

    @Override
    public Flux<ChatEventVO> chat(String userMessage, String chatId, String userId, String traceId) {
        agentTraceService.record(traceId, agentCode().name(), agentName(), "agent", "agent_start", "running", userMessage);
        if (!StringUtils.hasText(userMessage)) {
            agentTraceService.recordError(traceId, agentCode().name(), agentName(), "agent", "排障问题不能为空", null);
            return Flux.just(errorEvent("排障问题不能为空，请补充服务名、环境、traceId 或异常现象。"), ChatEventVO.stop());
        }

        String conversationId = normalizeConversationId(userId, chatId);
        String userPrompt = buildUserPrompt(userMessage);
        agentTraceService.record(traceId, agentCode().name(), agentName(), "prompt", "user_prompt_built", "success", userPrompt);

        Flux<ChatEventVO> contentStream = Flux.defer(() -> {
                    AgentTraceContext.setTraceId(traceId);
                    long start = System.currentTimeMillis();
                    return opsAgentClient.prompt()
                            .advisors(advisors -> advisors.param(conversationIdParam(), conversationId))
                            .user(userPrompt)
                            .stream()
                            .content()
                            .filter(Objects::nonNull)
                            .map(this::dataEvent)
                            .doOnComplete(() -> agentTraceService.record(traceId, agentCode().name(), agentName(),
                                    "agent", "agent_complete", "success", java.util.Map.of("durationMs", System.currentTimeMillis() - start)))
                            .doFinally(signalType -> AgentTraceContext.clear());
                });

        return appendStop(contentStream)
                .onErrorResume(ex -> {
                    agentTraceService.recordError(traceId, agentCode().name(), agentName(), "agent", "运维排障生成失败", ex);
                    return Flux.just(errorEvent("运维排障生成失败，请稍后重试或检查模型与 Tool 配置。"), ChatEventVO.stop());
                });
    }

    private String buildUserPrompt(String userMessage) {
        return """
                请根据用户描述判断需要调用哪些运维诊断 Tool，并基于 Tool 返回结果给出排障建议。
                如果用户信息不足，请直接说明缺少哪些字段，不要编造系统状态。

                【用户问题】
                %s
                """.formatted(userMessage);
    }
}
