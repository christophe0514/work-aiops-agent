package com.example.agent.core.service.impl;

import com.example.agent.core.domain.vo.ChatEventVO;
import com.example.agent.core.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    @Qualifier("operationQaAgentClient")
    private ChatClient chatClient;

    @Override
    public Flux<ChatEventVO> chat(String userMessage, String chatId, String userId) {
        String conversationId = userId + "_" + chatId;

        Flux<ChatEventVO> contentStream = chatClient.prompt()
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId)) // 设置上下文记忆的ID
                .user(userMessage)
                .stream()
                .content()
                .filter(Objects::nonNull)
                .map(ChatEventVO::data);

        return Flux.concat(contentStream, Flux.just(ChatEventVO.stop()))
                .onErrorResume(ex -> Flux.just(ChatEventVO.error("对话生成失败，请稍后重试或联系平台支持。")));
    }
}
