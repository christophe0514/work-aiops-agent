package com.example.agent.core.service;

import com.example.agent.core.domain.vo.ChatEventVO;
import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<ChatEventVO> chat(String userMessage);
}
