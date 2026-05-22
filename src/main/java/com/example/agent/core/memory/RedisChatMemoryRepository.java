package com.example.agent.core.memory;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

public class RedisChatMemoryRepository implements ChatMemoryRepository {
    private static final String KEY_PREFIX = "aiops:chat:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 获取所有的对话
    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (CollUtil.isEmpty(keys)) {
            return List.of();
        }
        return keys.stream().map(key -> key.replace(KEY_PREFIX, "")).toList();
    }

    // 根据对话ID获取某一条对话的上下文
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
     * 保存对话上下文
     *
     * @param conversationId 对话ID
     * @param messages       全量的上下文
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = getKey(conversationId);

        // 删除旧的上下文
        stringRedisTemplate.delete(key);

        List<String> messageJsonList = messages.stream()
                .map(MessageUtil::toJson)
                .toList();

        stringRedisTemplate.opsForList()
                .rightPushAll(key, messageJsonList);
    }

    // 根据对话ID删除对话
    @Override
    public void deleteByConversationId(String conversationId) {
        String key = getKey(conversationId);
        stringRedisTemplate.delete(key);
    }

    private String getKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }
}
