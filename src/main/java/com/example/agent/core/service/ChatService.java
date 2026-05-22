package com.example.agent.core.service;

import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<String> chat(String userMessage);
}
