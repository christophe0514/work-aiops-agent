package com.example.agent.core.router.impl;

import com.example.agent.core.agent.AgentCode;
import com.example.agent.core.domain.vo.AgentRouteResultVO;
import com.example.agent.core.router.AgentRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 基于大模型的 AgentRouter 实现。
 */
@Component
public class LlmAgentRouter implements AgentRouter {

    @Resource
    @Qualifier("agentRouterClient")
    private ChatClient agentRouterClient;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AgentRouteResultVO route(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return clarify("请先输入需要咨询的问题。");
        }

        String content = agentRouterClient.prompt()
                .user(buildRoutePrompt(userMessage))
                .call()
                .content();

        return parseRouteResult(content);
    }

    private String buildRoutePrompt(String userMessage) {
        return """
                请分析下面的用户问题，并输出一个 JSON 路由结果。

                用户问题：
                %s
                """.formatted(userMessage);
    }

    private AgentRouteResultVO parseRouteResult(String content) {
        try {
            String json = extractJson(content);
            JsonNode root = objectMapper.readTree(json);
            String rawAgentCode = root.path("agentCode").asText(AgentCode.AGENT_ROUTER.name());
            AgentCode agentCode = AgentCode.of(rawAgentCode);
            boolean needClarify = root.path("needClarify").asBoolean(false);

            return AgentRouteResultVO.builder()
                    .agentCode(agentCode.name())
                    .agentName(root.path("agentName").asText(agentCode.getName()))
                    .reason(root.path("reason").asText("AgentRouter 已完成问题意图分析。"))
                    .confidence(root.path("confidence").asDouble(0.0D))
                    .needClarify(needClarify)
                    .clarifyQuestion(root.path("clarifyQuestion").asText(null))
                    .build();
        } catch (Exception ex) {
            return clarify("我还不能确定这个问题应该交给哪个 Agent，请补充问题背景或说明你希望查询主题业务、工单处理还是运维排障。");
        }
    }

    private String extractJson(String content) {
        if (!StringUtils.hasText(content)) {
            return "{}";
        }

        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private AgentRouteResultVO clarify(String clarifyQuestion) {
        return AgentRouteResultVO.builder()
                .agentCode(AgentCode.AGENT_ROUTER.name())
                .agentName(AgentCode.AGENT_ROUTER.getName())
                .reason("用户问题信息不足，暂时无法稳定分派到具体业务 Agent。")
                .confidence(0.0D)
                .needClarify(true)
                .clarifyQuestion(clarifyQuestion)
                .build();
    }
}
