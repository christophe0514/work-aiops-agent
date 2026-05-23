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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    /**
     * 具体主题查询常见关键词。命中这类问题时，即使 RAG 没有检索结果，也要放行给模型调用 Tool。
     */
    private static final Pattern THEME_DATA_QUERY_PATTERN = Pattern.compile(
            "(主题|theme|资源包).*(状态|审核|上架|下架|可见|驳回|失败|原因|同步)|" +
                    "(状态|审核|上架|下架|可见|驳回|失败|原因|同步).*(主题|theme|资源包)",
            Pattern.CASE_INSENSITIVE
    );

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

        boolean themeDataQuery = isThemeDataQuery(userMessage);
        if (references.isEmpty() && !themeDataQuery) {
            return Flux.just(ChatEventVO.data(buildFallbackMessage()), ChatEventVO.stop());
        }

        String conversationId = normalizeConversationId(userId, chatId);
        Flux<ChatEventVO> contentStream = chatClient.prompt()
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(buildUserPrompt(userMessage, references, themeDataQuery))
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

    private boolean isThemeDataQuery(String userMessage) {
        return THEME_DATA_QUERY_PATTERN.matcher(userMessage).find();
    }

    private String buildFallbackMessage() {
        return "当前知识库没有明确说明，建议联系工号为 "
                + ragProperties.getFallbackOwnerEmployeeNo()
                + " 的业务接口人确认，或提交工单补充知识库资料。";
    }

    private String buildUserPrompt(String userMessage, List<KbSearchResultVO> references, boolean themeDataQuery) {
        if (references.isEmpty()) {
            return buildToolOnlyPrompt(userMessage);
        }
        return buildRagUserPrompt(userMessage, references, themeDataQuery);
    }

    private String buildToolOnlyPrompt(String userMessage) {
        return """
                用户的问题涉及具体主题业务数据，但知识库没有命中可参考资料。
                如果用户提供了主题ID，请调用主题业务 Tool 查询真实业务快照后回答。
                如果用户没有提供主题ID，请提示用户补充主题ID。

                【用户问题】
                %s
                """.formatted(userMessage);
    }

    private String buildRagUserPrompt(String userMessage, List<KbSearchResultVO> references, boolean themeDataQuery) {
        String context = references.stream()
                .map(this::formatReference)
                .collect(Collectors.joining("\n\n"));

        String toolInstruction = themeDataQuery
                ? "如果问题涉及具体主题ID的状态、审核、上架或驳回原因，必须调用主题业务 Tool 后再回答。"
                : "请只根据知识库资料回答；资料不足时说明“当前知识库没有明确说明”。";

        return """
                请根据下面的知识库资料和 Tool 使用规则回答用户问题，不要编造知识库或 Tool 以外的规则、状态或数据。
                %s
                如果资料不足以回答，请建议联系工号为 %s 的业务接口人。
                回答要面向运营、审核和客服人员，先给结论，再给必要步骤或注意事项。
                回答末尾请用“参考资料”列出命中的文档标题和来源路径。

                【知识库资料】
                %s

                【用户问题】
                %s
                """.formatted(toolInstruction, ragProperties.getFallbackOwnerEmployeeNo(), context, userMessage);
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
