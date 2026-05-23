package com.example.agent.core.memory;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

/**
 * 基于 Redis 的 Spring AI 会话记忆存储。
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "aiops:chat:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询所有会话 ID。
     */
    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (CollUtil.isEmpty(keys)) {
            return List.of();
        }
        return keys.stream().map(key -> key.replace(KEY_PREFIX, "")).toList();
    }

    /**
     * 根据会话 ID 查询历史消息。
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = getKey(conversationId);
        List<String> messages = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (CollUtil.isEmpty(messages)) {
            return List.of();
        }

        return CollStreamUtil.toList(messages, MessageUtil::toMessage);
    }

    /**
     * 保存某个会话的完整上下文窗口。
     *
     * @param conversationId 会话 ID
     * @param messages       完整消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = getKey(conversationId);

        // Spring AI 传入的是当前窗口完整消息，因此先删除旧列表，再写入新列表。
        stringRedisTemplate.delete(key);

        List<String> messageJsonList = messages.stream()
                .map(MessageUtil::toJson)
                .toList();

        stringRedisTemplate.opsForList()
                .rightPushAll(key, messageJsonList);
    }

    /**
     * 根据会话 ID 删除历史消息。
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        String key = getKey(conversationId);
        stringRedisTemplate.delete(key);
    }

    private String getKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }
}
