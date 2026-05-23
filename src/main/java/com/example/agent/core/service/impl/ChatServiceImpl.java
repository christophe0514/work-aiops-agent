package com.example.agent.core.service.impl;

import com.example.agent.core.config.RagProperties;
import com.example.agent.core.domain.vo.ChatEventVO;
import com.example.agent.core.domain.vo.KbSearchResultVO;
import com.example.agent.core.service.ChatService;
import com.example.agent.core.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    @Qualifier("operationQaAgentClient")
    private ChatClient chatClient;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private RagProperties ragProperties;

    @Override
    public Flux<ChatEventVO> chat(String userMessage, String chatId, String userId) {
        if (!StringUtils.hasText(userMessage)) {
            return Flux.just(ChatEventVO.error("用户问题不能为空。"), ChatEventVO.stop());
        }

        List<KbSearchResultVO> references;
        try {
            references = knowledgeBaseService.search(userMessage);
        } catch (Exception ex) {
            return Flux.just(ChatEventVO.error("知识库检索失败，请检查 Redis Stack、向量索引和 Embedding 配置。"), ChatEventVO.stop());
        }

        if (references.isEmpty()) {
            return Flux.just(ChatEventVO.data(buildFallbackMessage()), ChatEventVO.stop());
        }

        String conversationId = normalizeConversationId(userId, chatId);
        Flux<ChatEventVO> contentStream = chatClient.prompt()
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(buildRagUserPrompt(userMessage, references))
                .stream()
                .content()
                .filter(Objects::nonNull)
                .map(ChatEventVO::data);

        return Flux.concat(contentStream, Flux.just(ChatEventVO.stop()))
                .onErrorResume(ex -> Flux.just(ChatEventVO.error("对话生成失败，请稍后重试或联系平台支持。")));
    }

    private String normalizeConversationId(String userId, String chatId) {
        String normalizedUserId = hasText(userId) ? userId.trim() : "anonymous";
        String normalizedChatId = hasText(chatId) ? chatId.trim() : UUID.randomUUID().toString();
        return normalizedUserId + "_" + normalizedChatId;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildFallbackMessage() {
        return "当前知识库没有明确说明，建议联系工号为 "
                + ragProperties.getFallbackOwnerEmployeeNo()
                + " 的业务接口人确认，或提交工单补充知识库资料。";
    }

    private String buildRagUserPrompt(String userMessage, List<KbSearchResultVO> references) {
        String context = references.stream()
                .map(this::formatReference)
                .collect(Collectors.joining("\n\n"));

        return """
                请只根据下面的知识库资料回答用户问题，不要编造知识库以外的规则、状态或数据。
                如果资料不足以回答，请直接说明“当前知识库没有明确说明”，并建议联系工号为 %s 的业务接口人。
                回答要面向运营、审核和客服人员，先给结论，再给必要步骤或注意事项。
                回答末尾请用“参考资料”列出命中的文档标题和来源路径。

                【知识库资料】
                %s

                【用户问题】
                %s
                """.formatted(ragProperties.getFallbackOwnerEmployeeNo(), context, userMessage);
    }

    private String formatReference(KbSearchResultVO reference) {
        Map<String, Object> metadata = reference.getMetadata() == null ? Map.of() : reference.getMetadata();
        Object title = metadata.getOrDefault("title", "未知文档");
        Object sourcePath = metadata.getOrDefault("sourcePath", "未知来源");
        Object chunkIndex = metadata.getOrDefault("chunkIndex", "-");
        return """
                文档：%s
                来源：%s
                片段：%s
                相似度：%s
                内容：
                %s
                """.formatted(title, sourcePath, chunkIndex, reference.getScore(), reference.getContent());
    }
}
