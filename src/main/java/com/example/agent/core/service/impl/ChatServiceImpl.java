package com.example.agent.core.service.impl;

import com.example.agent.core.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    @Qualifier("operationQaAgentClient")
    private ChatClient chatClient;

    @Override
    public Flux<String> chat(String userMessage) {
        return chatClient.prompt().user(userMessage).stream().content();
    }
}
