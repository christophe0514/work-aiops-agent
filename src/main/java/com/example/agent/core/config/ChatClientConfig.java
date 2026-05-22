package com.example.agent.core.config;

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

    // 负责：给运营答疑、解释平台功能、流程说明、常见问题
    @Bean
    public ChatClient operationQaAgentClient(ChatClient.Builder chatClient,
                                             MessageChatMemoryAdvisor messageChatMemoryAdvisor, // 上下文记忆增强
                                             SimpleLoggerAdvisor loggerAdvisor // 日志增强
    ) {
        return chatClient
                .defaultSystem(promptManager.getSystemPrompt("OperationQaAgent")) // 设置系统提示词
                .defaultAdvisors(messageChatMemoryAdvisor, loggerAdvisor) // 设置增强器
                .build();
    }

    // 负责：上架失败、系统异常、日志排查、流水线问题
    @Bean
    public ChatClient OpsAgent(ChatClient.Builder chatClient) {
        // todo
        return null;
    }

    // 负责：工单分类、优先级判断、分配开发组、生成处理建议
    @Bean
    public ChatClient TicketAgent(ChatClient.Builder chatClient) {
        // todo
        return null;
    }

}
