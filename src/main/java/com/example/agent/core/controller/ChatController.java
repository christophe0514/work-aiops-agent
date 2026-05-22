package com.example.agent.core.controller;

import com.example.agent.core.domain.dto.ChatMessageDTO;
import com.example.agent.core.domain.vo.ChatEventVO;
import com.example.agent.core.service.ChatService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatMessageDTO chatMessageDTO) {
        return chatService.chat(chatMessageDTO.getUserMessage());
    }

}
