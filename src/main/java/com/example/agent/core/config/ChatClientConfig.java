package com.example.agent.core.config;

import com.example.agent.tools.theme.ThemeBusinessTools;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Resource
    private PromptManager promptManager;

    /**
     * 主题业务 Agent：负责运营答疑、流程解释、规则说明，以及通过 Tool 查询具体主题业务状态。
     */
    @Bean
    public ChatClient operationQaAgentClient(ChatClient.Builder chatClient,
                                             MessageChatMemoryAdvisor messageChatMemoryAdvisor,
                                             SimpleLoggerAdvisor loggerAdvisor,
                                             ThemeBusinessTools themeBusinessTools) {
        return chatClient
                .defaultSystem(buildOperationQaSystemPrompt())
                .defaultTools(themeBusinessTools)
                .defaultAdvisors(messageChatMemoryAdvisor, loggerAdvisor)
                .build();
    }

    /**
     * 在数据库 Prompt 基础上补充 Tool 使用边界，避免模型对具体主题状态做猜测。
     */
    private String buildOperationQaSystemPrompt() {
        return promptManager.getSystemPrompt("OperationQaAgent")
                + """

                【主题业务 Tool 使用规则】
                1. 当用户询问具体主题ID的主题状态、审核状态、上架状态、可见渠道、驳回原因或上架失败原因时，必须优先调用主题业务 Tool 查询。
                2. Tool 返回的是当前模拟业务数据，回答时要明确基于查询结果说明，不要虚构 Tool 未返回的字段。
                3. 如果 Tool 返回上架失败且原因涉及资源同步、CDN、流水线、接口异常，应建议转交 OpsAgent 继续排查。
                4. 如果用户没有提供主题ID，应先提示用户补充主题ID，不要自行编造。
                """;
    }

    /**
     * 运维排障 Agent：后续负责上架失败、系统异常、日志排查、流水线问题。
     */
    @Bean
    public ChatClient OpsAgent(ChatClient.Builder chatClient) {
        // todo
        return null;
    }

    /**
     * 工单 Agent：后续负责工单分类、优先级判断、开发组分派、处理建议生成。
     */
    @Bean
    public ChatClient TicketAgent(ChatClient.Builder chatClient) {
        // todo
        return null;
    }
}
