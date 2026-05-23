package com.example.agent.core.agent.impl;

import com.example.agent.core.agent.AbstractAgent;
import com.example.agent.core.agent.AgentCode;
import com.example.agent.core.domain.vo.ChatEventVO;
import com.example.agent.rag.config.RagProperties;
import com.example.agent.rag.domain.vo.KbSearchResultVO;
import com.example.agent.rag.service.KnowledgeBaseService;
import com.example.agent.tools.theme.client.ThemeBusinessClient;
import com.example.agent.tools.theme.domain.vo.ThemeBusinessSnapshotVO;
import com.example.agent.trace.context.AgentTraceContext;
import com.example.agent.trace.service.AgentTraceService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 主题业务 Agent。
 *
 * <p>负责主题创作者平台运营规则、业务流程、FAQ，以及具体主题 ID 的业务状态查询。</p>
 */
@Component
public class OperationQaAgent extends AbstractAgent {

    /**
     * 具体主题查询常见关键词。命中这类问题时，即使 RAG 没有结果，也允许继续处理主题业务数据查询。
     */
    private static final Pattern THEME_DATA_QUERY_PATTERN = Pattern.compile(
            "(主题|theme|资源包).*(状态|审核|上架|下架|可见|驳回|失败|原因|同步)|" +
                    "(状态|审核|上架|下架|可见|驳回|失败|原因|同步).*(主题|theme|资源包)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern THEME_ID_PATTERN = Pattern.compile("(?i)\\btheme[_-]?\\d+\\b|\\b\\d{4,}\\b");

    @Resource
    @Qualifier("operationQaAgentClient")
    private ChatClient chatClient;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private RagProperties ragProperties;

    @Resource
    private AgentTraceService agentTraceService;

    @Resource
    private ThemeBusinessClient themeBusinessClient;

    @Override
    public AgentCode agentCode() {
        return AgentCode.OPERATION_QA;
    }

    @Override
    public String agentName() {
        return AgentCode.OPERATION_QA.getName();
    }

    @Override
    public Flux<ChatEventVO> chat(String userMessage, String chatId, String userId, String traceId) {
        agentTraceService.record(traceId, agentCode().name(), agentName(), "agent", "agent_start", "running", userMessage);
        if (!StringUtils.hasText(userMessage)) {
            agentTraceService.recordError(traceId, agentCode().name(), agentName(), "agent", "用户问题不能为空。", null);
            return Flux.just(errorEvent("用户问题不能为空。"), ChatEventVO.stop());
        }

        List<KbSearchResultVO> references;
        try {
            references = knowledgeBaseService.search(userMessage);
            agentTraceService.record(traceId, agentCode().name(), agentName(), "rag", "knowledge_search", "success", references);
        } catch (Exception ex) {
            agentTraceService.recordError(traceId, agentCode().name(), agentName(), "rag", "知识库检索失败", ex);
            return Flux.just(errorEvent("知识库检索失败，请检查 Redis Stack、向量索引和 Embedding 配置。"), ChatEventVO.stop());
        }

        boolean themeDataQuery = isThemeDataQuery(userMessage);
        agentTraceService.record(traceId, agentCode().name(), agentName(), "intent", "theme_data_query_detected", "success", themeDataQuery);
        ThemeSnapshotLookup snapshotLookup = lookupThemeSnapshotIfNeeded(userMessage, themeDataQuery, traceId);

        if (references.isEmpty() && !themeDataQuery) {
            agentTraceService.record(traceId, agentCode().name(), agentName(), "fallback", "knowledge_miss_fallback", "success", buildFallbackMessage());
            return Flux.just(dataEvent(buildFallbackMessage()), ChatEventVO.stop());
        }

        String conversationId = normalizeConversationId(userId, chatId);
        String userPrompt = buildUserPrompt(userMessage, references, themeDataQuery, snapshotLookup);
        agentTraceService.record(traceId, agentCode().name(), agentName(), "prompt", "user_prompt_built", "success", userPrompt);

        Flux<ChatEventVO> contentStream = Flux.defer(() -> {
            AgentTraceContext.setTraceId(traceId);
            long start = System.currentTimeMillis();
            return chatClient.prompt()
                    .advisors(advisors -> advisors.param(conversationIdParam(), conversationId))
                    .user(userPrompt)
                    .stream()
                    .content()
                    .filter(Objects::nonNull)
                    .map(this::dataEvent)
                    .doOnComplete(() -> agentTraceService.record(traceId, agentCode().name(), agentName(),
                            "agent", "agent_complete", "success", Map.of("durationMs", System.currentTimeMillis() - start)))
                    .doFinally(signalType -> AgentTraceContext.clear());
        });

        return appendStop(contentStream)
                .onErrorResume(ex -> {
                    agentTraceService.recordError(traceId, agentCode().name(), agentName(), "agent", "对话生成失败", ex);
                    return Flux.just(errorEvent("对话生成失败，请稍后重试或联系平台支持。"), ChatEventVO.stop());
                });
    }

    private boolean isThemeDataQuery(String userMessage) {
        return THEME_DATA_QUERY_PATTERN.matcher(userMessage).find();
    }

    private ThemeSnapshotLookup lookupThemeSnapshotIfNeeded(String userMessage, boolean themeDataQuery, String traceId) {
        if (!themeDataQuery) {
            return ThemeSnapshotLookup.skipped("not_theme_data_query");
        }

        Optional<String> themeId = extractThemeId(userMessage);
        if (themeId.isEmpty()) {
            agentTraceService.record(traceId, agentCode().name(), agentName(), "tool_guard", "theme_id_missing", "success", userMessage);
            return ThemeSnapshotLookup.missingThemeId();
        }

        String resolvedThemeId = themeId.get();
        long start = System.currentTimeMillis();
        agentTraceService.recordToolCall(traceId, agentCode().name(), agentName(), "forced_queryThemeBusinessSnapshot", Map.of("themeId", resolvedThemeId));
        try {
            ThemeBusinessSnapshotVO snapshot = themeBusinessClient.queryThemeBusinessSnapshot(resolvedThemeId);
            agentTraceService.recordToolResult(traceId, agentCode().name(), agentName(), "forced_queryThemeBusinessSnapshot", snapshot, System.currentTimeMillis() - start);
            return ThemeSnapshotLookup.success(resolvedThemeId, snapshot);
        } catch (Exception ex) {
            agentTraceService.recordError(traceId, agentCode().name(), agentName(), "tool_guard", "forced_queryThemeBusinessSnapshot failed", ex);
            return ThemeSnapshotLookup.degraded(resolvedThemeId, "主题业务系统暂时不可用，已降级为基于知识库规则回答。");
        }
    }

    private Optional<String> extractThemeId(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return Optional.empty();
        }
        Matcher matcher = THEME_ID_PATTERN.matcher(userMessage);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group());
    }

    private String buildFallbackMessage() {
        return "当前知识库没有明确说明，建议联系工号为 "
                + ragProperties.getFallbackOwnerEmployeeNo()
                + " 的业务接口人确认，或提交工单补充知识库资料。";
    }

    private String buildUserPrompt(String userMessage, List<KbSearchResultVO> references, boolean themeDataQuery, ThemeSnapshotLookup snapshotLookup) {
        if (references.isEmpty()) {
            return buildToolOnlyPrompt(userMessage, snapshotLookup);
        }
        return buildRagUserPrompt(userMessage, references, themeDataQuery, snapshotLookup);
    }

    private String buildToolOnlyPrompt(String userMessage, ThemeSnapshotLookup snapshotLookup) {
        return """
                用户的问题涉及具体主题业务数据，但知识库没有命中可参考资料。
                后端已优先执行确定性主题业务快照查询，请优先基于【主题业务快照】回答。
                如果【主题业务快照】提示业务系统不可用，请明确说明当前只能基于通用规则降级回答，不要编造实时状态。
                如果用户没有提供主题 ID，请提示用户补充主题 ID。

                【主题业务快照】
                %s

                【用户问题】
                %s
                """.formatted(formatSnapshotLookup(snapshotLookup), userMessage);
    }

    private String buildRagUserPrompt(String userMessage, List<KbSearchResultVO> references, boolean themeDataQuery, ThemeSnapshotLookup snapshotLookup) {
        String context = references.stream()
                .map(this::formatReference)
                .collect(Collectors.joining("\n\n"));

        String toolInstruction = themeDataQuery
                ? "如果问题涉及具体主题 ID 的状态、审核、上架或驳回原因，后端已优先执行确定性主题业务快照查询。请结合【主题业务快照】和知识库规则回答；如果快照降级或缺失，不要编造实时状态。"
                : "请只根据知识库资料回答；资料不足时说明“当前知识库没有明确说明”。";

        return """
                请根据下面的知识库资料和 Tool 使用规则回答用户问题，不要编造知识库或 Tool 以外的规则、状态或数据。
                %s
                如果资料不足以回答，请建议联系工号为 %s 的业务接口人。
                回答要面向运营、审核和客服人员，先给结论，再给必要步骤或注意事项。
                回答末尾请用“参考资料”列出命中的文档标题和来源路径。

                【主题业务快照】
                %s

                【知识库资料】
                %s

                【用户问题】
                %s
                """.formatted(toolInstruction, ragProperties.getFallbackOwnerEmployeeNo(), formatSnapshotLookup(snapshotLookup), context, userMessage);
    }

    private String formatSnapshotLookup(ThemeSnapshotLookup snapshotLookup) {
        if (snapshotLookup == null || snapshotLookup.status() == SnapshotLookupStatus.SKIPPED) {
            return "本轮问题不需要查询具体主题业务快照。";
        }
        if (snapshotLookup.status() == SnapshotLookupStatus.MISSING_THEME_ID) {
            return "用户问题涉及具体主题数据，但未识别到主题 ID。请提示用户补充主题 ID。";
        }
        if (snapshotLookup.status() == SnapshotLookupStatus.DEGRADED) {
            return """
                    查询状态：降级
                    主题ID：%s
                    降级原因：%s
                    回答要求：只能基于知识库规则和通用处理流程回答，不要编造该主题的实时审核、上架或可见状态。
                    """.formatted(snapshotLookup.themeId(), snapshotLookup.degradeReason());
        }

        ThemeBusinessSnapshotVO snapshot = snapshotLookup.snapshot();
        return """
                查询状态：成功
                主题ID：%s
                主题名称：%s
                创作者ID：%s
                主题状态：%s
                审核状态：%s
                上架状态：%s
                可见渠道：%s
                原因说明：%s
                处理建议：%s
                最近更新时间：%s
                最近审核记录：%s
                """.formatted(
                snapshot.getThemeId(),
                snapshot.getThemeName(),
                snapshot.getCreatorId(),
                snapshot.getThemeStatus(),
                snapshot.getAuditStatus(),
                snapshot.getPublishStatus(),
                snapshot.getVisibleChannels(),
                snapshot.getReason(),
                snapshot.getSuggestion(),
                snapshot.getUpdatedTime(),
                snapshot.getAuditRecords()
        );
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

    private enum SnapshotLookupStatus {
        SKIPPED,
        MISSING_THEME_ID,
        SUCCESS,
        DEGRADED
    }

    private record ThemeSnapshotLookup(
            SnapshotLookupStatus status,
            String themeId,
            ThemeBusinessSnapshotVO snapshot,
            String degradeReason
    ) {

        static ThemeSnapshotLookup skipped(String reason) {
            return new ThemeSnapshotLookup(SnapshotLookupStatus.SKIPPED, null, null, reason);
        }

        static ThemeSnapshotLookup missingThemeId() {
            return new ThemeSnapshotLookup(SnapshotLookupStatus.MISSING_THEME_ID, null, null, "missing_theme_id");
        }

        static ThemeSnapshotLookup success(String themeId, ThemeBusinessSnapshotVO snapshot) {
            return new ThemeSnapshotLookup(SnapshotLookupStatus.SUCCESS, themeId, snapshot, null);
        }

        static ThemeSnapshotLookup degraded(String themeId, String reason) {
            return new ThemeSnapshotLookup(SnapshotLookupStatus.DEGRADED, themeId, null, reason);
        }
    }
}
