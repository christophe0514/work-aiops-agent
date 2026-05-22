package com.example.agent.core.config;

import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用于存放AI工程的公共底座能力，如上下文管理、日志增强等
 */
@Configuration
public class AiConfig {

    /**
     * 日志记录器
     */
    @Bean
    public SimpleLoggerAdvisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

}
