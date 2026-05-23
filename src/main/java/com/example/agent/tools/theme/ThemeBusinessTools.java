package com.example.agent.tools.theme;

import com.example.agent.tools.theme.client.ThemeBusinessClient;
import com.example.agent.tools.theme.domain.vo.ThemeBusinessSnapshotVO;
import com.example.agent.trace.context.AgentTraceContext;
import com.example.agent.trace.service.AgentTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 主题业务 Agent 可调用的业务查询工具。
 *
 * <p>这些方法会暴露给大模型作为 Tool。当前底层使用 Mock 客户端返回模拟数据，
 * 后续切换到真实 HTTP 服务后，Tool 方法签名和 Agent 调用方式可以保持不变。
 */
@Component
@RequiredArgsConstructor
public class ThemeBusinessTools {

    private final ThemeBusinessClient themeBusinessClient;

    private final AgentTraceService agentTraceService;

    /**
     * 查询主题状态、审核状态、上架状态等聚合信息。
     *
     * @param themeId 用户提供的主题 ID
     * @return 主题业务快照
     */
    @Tool(
            name = "queryThemeBusinessSnapshot",
            description = "根据主题ID查询主题业务快照，包括主题状态、审核状态、上架状态、可见渠道、失败原因和处理建议。"
    )
    public ThemeBusinessSnapshotVO queryThemeBusinessSnapshot(
            @ToolParam(description = "主题ID，例如 theme_10001、10001") String themeId) {
        String traceId = AgentTraceContext.getTraceId();
        long start = System.currentTimeMillis();
        agentTraceService.recordToolCall(traceId, "OPERATION_QA", "运营问答 Agent", "queryThemeBusinessSnapshot", args("themeId", themeId));
        try {
            ThemeBusinessSnapshotVO result = themeBusinessClient.queryThemeBusinessSnapshot(themeId);
            agentTraceService.recordToolResult(traceId, "OPERATION_QA", "运营问答 Agent", "queryThemeBusinessSnapshot", result, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException ex) {
            agentTraceService.recordError(traceId, "OPERATION_QA", "运营问答 Agent", "tool", "queryThemeBusinessSnapshot 调用失败", ex);
            throw ex;
        }
    }

    private Map<String, Object> args(String key, Object value) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put(key, value);
        return args;
    }
}
