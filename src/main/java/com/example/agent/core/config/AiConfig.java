package com.example.agent.core.config;

import com.example.agent.core.memory.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用于存放AI工程的公共底座能力，如上下文管理、日志增强等
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class AiConfig {

    /**
     * 日志记录器
     */
    @Bean
    public SimpleLoggerAdvisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    /**
     * Redis上下文存储实现
     */
    @Bean
    public ChatMemoryRepository redisChatMemoryRepository() {
        return new RedisChatMemoryRepository();
    }

    /**
     * 上下文记忆
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).maxMessages(20).build();
    }

    /**
     * 上下文记忆存储增强
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
