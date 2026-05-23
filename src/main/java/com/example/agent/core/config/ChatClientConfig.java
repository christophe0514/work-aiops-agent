package com.example.agent.core.config;

import com.example.agent.tools.theme.ThemeBusinessTools;
import com.example.agent.tools.ops.OpsDiagnosticTools;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置。
 *
 * <p>不同 Agent 使用独立 ChatClient，便于后续分别维护系统提示词、Tool 和上下文策略。</p>
 */
@Configuration
public class ChatClientConfig {

    @Resource
    private PromptManager promptManager;

    /**
     * LLM 路由 Agent：只做意图识别和 Agent 分派，不直接回答业务问题。
     */
    @Bean
    public ChatClient agentRouterClient(ChatClient.Builder chatClient,
                                        SimpleLoggerAdvisor loggerAdvisor) {
        return chatClient
                .defaultSystem(buildAgentRouterSystemPrompt())
                .defaultAdvisors(loggerAdvisor)
                .build();
    }

    /**
     * 主题业务 Agent：负责运营答疑、流程解释、规则说明，以及通过 Tool 查询具体主题业务状态。
     */
    @Bean
    public ChatClient operationQaAgentClient(ChatClient.Builder chatClient,
                                             MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                             SimpleLoggerAdvisor loggerAdvisor,
                                             ThemeBusinessTools themeBusinessTools) {
        return chatClient
                .defaultSystem(promptManager.getSystemPrompt("OperationQaAgent"))
                .defaultTools(themeBusinessTools)
                .defaultAdvisors(messageChatMemoryAdvisor, loggerAdvisor)
                .build();
    }

    /**
     * 运维排障 Agent：负责接口异常、服务健康、告警、Trace、流水线等技术排障问题。
     */
    @Bean
    public ChatClient opsAgentClient(ChatClient.Builder chatClient,
                                     MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                     SimpleLoggerAdvisor loggerAdvisor,
                                     OpsDiagnosticTools opsDiagnosticTools) {
        return chatClient
                .defaultSystem(buildOpsAgentSystemPrompt())
                .defaultTools(opsDiagnosticTools)
                .defaultAdvisors(messageChatMemoryAdvisor, loggerAdvisor)
                .build();
    }

    private String buildAgentRouterSystemPrompt() {
        String prompt = promptManager.getSystemPrompt("RouterAgent");
        if (prompt != null && !prompt.isBlank()) {
            return prompt;
        }

        return """
                你是 AIOps Agent 系统的入口路由 Agent，只负责分析用户问题并选择目标业务 Agent。
                你不能直接回答业务问题，也不能编造业务数据。你必须只输出一个 JSON 对象，不要输出 Markdown。

                可选目标 Agent：
                1. OPERATION_QA：主题创作者平台运营规则、主题上传、审核、上架、下架、资源同步、主题状态、具体主题 ID 查询。
                2. TICKET：工单分类、工单摘要、优先级判断、处理部门或开发组推荐、工单处理建议。
                3. OPS：系统异常、接口超时、日志排查、告警、流水线、数据库、中间件、缓存、MQ、资源同步失败等运维排障问题。
                4. AGENT_ROUTER：问题过于模糊，需要先向用户澄清。

                输出 JSON 格式：
                {
                  "agentCode": "OPERATION_QA | TICKET | OPS | AGENT_ROUTER",
                  "agentName": "目标 Agent 中文名称",
                  "reason": "路由原因",
                  "confidence": 0.9,
                  "needClarify": false,
                  "clarifyQuestion": null
                }
                """;
    }

    private String buildOpsAgentSystemPrompt() {
        String prompt = promptManager.getSystemPrompt("OpsAgent");
        if (prompt != null && !prompt.isBlank()) {
            return prompt;
        }

        return """
                你是主题平台的运维排障 Agent，主要服务对象是开发、运维、技术支持人员。

                你的职责：
                1. 分析接口异常、服务异常、发布流水线异常、资源同步异常、告警和 Trace 调用链问题。
                2. 根据用户提供的服务名、环境、traceId、应用名或时间范围，调用运维诊断 Tool 获取模拟诊断数据。
                3. 基于 Tool 返回结果给出清晰、可执行的排查结论和下一步建议。

                工具使用规则：
                1. 涉及服务健康、错误率、耗时、实例异常时，优先调用 queryServiceHealth。
                2. 涉及告警、错误率升高、耗时升高时，优先调用 queryRecentAlerts。
                3. 涉及发布失败、流水线失败、资源同步失败时，优先调用 queryPipelineStatus。
                4. 涉及 traceId、接口超时、接口报错、调用链异常时，优先调用 queryTraceSummary。
                5. 如果用户没有提供必要信息，要先说明缺什么信息，并给出可补充字段，例如服务名、环境、traceId、时间范围。

                回答格式：
                - 先给“结论”。
                - 再给“依据”，引用 Tool 返回的关键指标或状态。
                - 最后给“建议步骤”，按优先级列出 3 到 5 步。
                - 不要虚构 Tool 没有返回的日志、告警或真实系统状态。
                """;
    }
}
