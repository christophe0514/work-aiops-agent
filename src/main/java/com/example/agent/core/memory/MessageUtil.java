package com.example.agent.core.memory;

import cn.hutool.json.JSONUtil;
import com.example.agent.core.memory.dto.MemoryMessageDTO;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring AI 消息与 Redis JSON 存储格式之间的转换工具。
 */
public class MessageUtil {

    private MessageUtil() {
    }

    /**
     * 将 Spring AI Message 转换为 Redis 中保存的 JSON 字符串。
     *
     * @param message 原始消息对象
     * @return JSON 字符串
     */
    public static String toJson(Message message) {
        MemoryMessageDTO memoryMessageDTO = MemoryMessageDTO.builder()
                // USER / ASSISTANT / SYSTEM / TOOL
                .type(message.getMessageType().name())
                // 消息文本内容
                .content(message.getText())
                .metadata(message.getMetadata())
                .createTime(LocalDateTime.now())
                .build();

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            memoryMessageDTO.setToolResponse(JSONUtil.toJsonStr(toolResponseMessage));
        }

        return JSONUtil.toJsonStr(memoryMessageDTO);
    }

    /**
     * 将 Redis 中保存的 JSON 字符串反序列化为 Spring AI Message。
     *
     * @param json Redis 中保存的消息 JSON
     * @return Spring AI Message
     */
    public static Message toMessage(String json) {
        try {
            MemoryMessageDTO memoryMessageDTO = JSONUtil.toBean(json, MemoryMessageDTO.class);
            MessageType messageType = MessageType.valueOf(memoryMessageDTO.getType());

            return switch (messageType) {
                case USER -> new UserMessage(memoryMessageDTO.getContent());
                case ASSISTANT -> new AssistantMessage(memoryMessageDTO.getContent());
                case SYSTEM -> new SystemMessage(memoryMessageDTO.getContent());
                case TOOL -> new ToolResponseMessage(List.of());
                default -> throw new IllegalArgumentException("unknown message type");
            };
        } catch (Exception e) {
            throw new RuntimeException("message deserialize failed", e);
        }
    }
}
