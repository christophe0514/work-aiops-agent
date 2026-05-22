package com.example.agent.core.memory;

import cn.hutool.json.JSONUtil;
import com.example.agent.core.memory.dto.MemoryMessageDTO;
import org.springframework.ai.chat.messages.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息转换工具类，提供消息对象与JSON字符串之间的转换功能，主要用于Redis存储格式转换
 */
public class MessageUtil {

    // 工具类私有化构造方法
    private MessageUtil() {}

    /**
     * 将Message对象转换为Redis存储格式的JSON字符串
     *
     * @param message 需要转换的原始消息对象
     * @return 符合Redis存储规范的JSON字符串
     */
    public static String toJson(Message message) {
        MemoryMessageDTO memoryMessageDTO = MemoryMessageDTO.builder()
                // USER / ASSISTANT / SYSTEM / TOOL
                .type(message.getMessageType().name())

                // 消息内容
                .content(message.getText())

                // metadata
                .metadata(message.getMetadata())

                // 创建时间
                .createTime(LocalDateTime.now())

                .build();

        // ToolMessage 特殊处理
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            memoryMessageDTO.setToolResponse(JSONUtil.toJsonStr(toolResponseMessage));
        }

        return JSONUtil.toJsonStr(memoryMessageDTO);
    }

    /**
     * 将Redis存储的JSON字符串反序列化为对应的Message对象
     *
     * @param json Redis存储的JSON格式消息数据
     * @return 对应类型的Message对象
     * @throws RuntimeException 当无法识别的消息类型时抛出异常
     */
    public static Message toMessage(String json) {
        try {
            MemoryMessageDTO memoryMessageDTO = JSONUtil.toBean(json, MemoryMessageDTO.class);
            MessageType messageType = MessageType.valueOf(memoryMessageDTO.getType());

            return switch (messageType) {

                case USER ->
                        new UserMessage(memoryMessageDTO.getContent());

                case ASSISTANT ->
                        new AssistantMessage(memoryMessageDTO.getContent());

                case SYSTEM ->
                        new SystemMessage(memoryMessageDTO.getContent());

                case TOOL ->
                        new ToolResponseMessage(List.of());
                default ->
                        throw new IllegalArgumentException(
                                "unknown message type"
                        );
            };
        } catch (Exception e) {
            throw new RuntimeException(
                    "message deserialize failed",
                    e
            );
        }
    }
}
