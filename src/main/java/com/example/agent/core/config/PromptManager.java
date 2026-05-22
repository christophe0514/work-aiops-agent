package com.example.agent.core.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.agent.core.domain.entity.AiPromptConfig;
import com.example.agent.core.service.AiPromptService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PromptManager {

    private final AiPromptService aiPromptService;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initAiPromptConfig() {
        List<AiPromptConfig> prompts =
                aiPromptService.list(new LambdaQueryWrapper<AiPromptConfig>().eq(AiPromptConfig::getIsEnabled, 1));

        for (AiPromptConfig prompt : prompts) {

            cache.put(buildKey(prompt), prompt.getPromptContent());
        }
    }

    private String buildKey(AiPromptConfig prompt) {
        return prompt.getAgentName()
                + ":"
                + prompt.getPromptType();
    }

    public String getSystemPrompt(String agentName) {
        return cache.get(agentName + ":system");
    }
}