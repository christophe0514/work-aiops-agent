package com.example.agent.core.config;

import com.example.agent.core.memory.RedisChatMemoryRepository;
import com.example.agent.rag.config.RagProperties;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 工程公共底座配置，例如日志增强、上下文记忆和 RAG 参数绑定。
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class AiConfig {

    /**
     * Spring AI 简单日志增强器，用于观察模型请求和响应。
     */
    @Bean
    public SimpleLoggerAdvisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    /**
     * Redis 上下文存储实现。
     */
    @Bean
    public ChatMemoryRepository redisChatMemoryRepository() {
        return new RedisChatMemoryRepository();
    }

    /**
     * 会话窗口记忆，限制注入模型的历史消息数量。
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).maxMessages(20).build();
    }

    /**
     * 将会话记忆挂载到 ChatClient 的 Advisor。
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
