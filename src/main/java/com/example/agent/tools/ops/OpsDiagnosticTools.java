package com.example.agent.tools.ops;

import com.example.agent.tools.ops.client.OpsDiagnosticClient;
import com.example.agent.tools.ops.domain.vo.OpsAlertSummaryVO;
import com.example.agent.tools.ops.domain.vo.OpsPipelineStatusVO;
import com.example.agent.tools.ops.domain.vo.OpsServiceHealthVO;
import com.example.agent.tools.ops.domain.vo.OpsTraceSummaryVO;
import com.example.agent.trace.context.AgentTraceContext;
import com.example.agent.trace.service.AgentTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维排障 Agent 可调用的诊断工具。
 */
@Component
@RequiredArgsConstructor
public class OpsDiagnosticTools {

    private final OpsDiagnosticClient opsDiagnosticClient;

    private final AgentTraceService agentTraceService;

    /**
     * 查询服务健康状态。
     */
    @Tool(
            name = "queryServiceHealth",
            description = "根据服务名称和环境查询服务健康状态，包括错误率、P95耗时、CPU、内存和实例状态。"
    )
    public OpsServiceHealthVO queryServiceHealth(
            @ToolParam(description = "服务名称，例如 theme-publish-service、theme-sync-service") String serviceName,
            @ToolParam(description = "环境，例如 test、pre、prod") String env) {
        String traceId = AgentTraceContext.getTraceId();
        long start = System.currentTimeMillis();
        agentTraceService.recordToolCall(traceId, "OPS", "运维排障 Agent", "queryServiceHealth", args("serviceName", serviceName, "env", env));
        try {
            OpsServiceHealthVO result = opsDiagnosticClient.queryServiceHealth(serviceName, env);
            agentTraceService.recordToolResult(traceId, "OPS", "运维排障 Agent", "queryServiceHealth", result, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException ex) {
            agentTraceService.recordError(traceId, "OPS", "运维排障 Agent", "tool", "queryServiceHealth 调用失败", ex);
            throw ex;
        }
    }

    /**
     * 查询近期告警。
     */
    @Tool(
            name = "queryRecentAlerts",
            description = "根据服务名称和时间范围查询近期告警摘要，适合分析错误率升高、耗时升高、实例异常等问题。"
    )
    public OpsAlertSummaryVO queryRecentAlerts(
            @ToolParam(description = "服务名称，例如 theme-publish-service") String serviceName,
            @ToolParam(description = "时间范围，例如 最近30分钟、最近1小时") String timeRange) {
        String traceId = AgentTraceContext.getTraceId();
        long start = System.currentTimeMillis();
        agentTraceService.recordToolCall(traceId, "OPS", "运维排障 Agent", "queryRecentAlerts", args("serviceName", serviceName, "timeRange", timeRange));
        try {
            OpsAlertSummaryVO result = opsDiagnosticClient.queryRecentAlerts(serviceName, timeRange);
            agentTraceService.recordToolResult(traceId, "OPS", "运维排障 Agent", "queryRecentAlerts", result, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException ex) {
            agentTraceService.recordError(traceId, "OPS", "运维排障 Agent", "tool", "queryRecentAlerts 调用失败", ex);
            throw ex;
        }
    }

    /**
     * 查询发布流水线状态。
     */
    @Tool(
            name = "queryPipelineStatus",
            description = "根据应用名称和环境查询最近一次发布流水线状态，适合排查发布失败、资源同步失败、回归检查失败。"
    )
    public OpsPipelineStatusVO queryPipelineStatus(
            @ToolParam(description = "应用名称，例如 theme-publish-service") String appName,
            @ToolParam(description = "环境，例如 test、pre、prod") String env) {
        String traceId = AgentTraceContext.getTraceId();
        long start = System.currentTimeMillis();
        agentTraceService.recordToolCall(traceId, "OPS", "运维排障 Agent", "queryPipelineStatus", args("appName", appName, "env", env));
        try {
            OpsPipelineStatusVO result = opsDiagnosticClient.queryPipelineStatus(appName, env);
            agentTraceService.recordToolResult(traceId, "OPS", "运维排障 Agent", "queryPipelineStatus", result, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException ex) {
            agentTraceService.recordError(traceId, "OPS", "运维排障 Agent", "tool", "queryPipelineStatus 调用失败", ex);
            throw ex;
        }
    }

    /**
     * 查询 Trace 调用链摘要。
     */
    @Tool(
            name = "queryTraceSummary",
            description = "根据 traceId 查询调用链摘要，适合排查接口超时、接口报错、下游依赖异常。"
    )
    public OpsTraceSummaryVO queryTraceSummary(
            @ToolParam(description = "调用链 traceId") String traceId) {
        String agentTraceId = AgentTraceContext.getTraceId();
        long start = System.currentTimeMillis();
        agentTraceService.recordToolCall(agentTraceId, "OPS", "运维排障 Agent", "queryTraceSummary", args("traceId", traceId));
        try {
            OpsTraceSummaryVO result = opsDiagnosticClient.queryTraceSummary(traceId);
            agentTraceService.recordToolResult(agentTraceId, "OPS", "运维排障 Agent", "queryTraceSummary", result, System.currentTimeMillis() - start);
            return result;
        } catch (RuntimeException ex) {
            agentTraceService.recordError(agentTraceId, "OPS", "运维排障 Agent", "tool", "queryTraceSummary 调用失败", ex);
            throw ex;
        }
    }

    private Map<String, Object> args(String key, Object value) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put(key, value);
        return args;
    }

    private Map<String, Object> args(String firstKey, Object firstValue, String secondKey, Object secondValue) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put(firstKey, firstValue);
        args.put(secondKey, secondValue);
        return args;
    }
}
