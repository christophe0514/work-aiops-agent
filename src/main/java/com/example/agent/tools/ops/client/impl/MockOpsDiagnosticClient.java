package com.example.agent.tools.ops.client.impl;

import com.example.agent.tools.ops.client.OpsDiagnosticClient;
import com.example.agent.tools.ops.domain.vo.OpsAlertItemVO;
import com.example.agent.tools.ops.domain.vo.OpsAlertSummaryVO;
import com.example.agent.tools.ops.domain.vo.OpsInstanceStatusVO;
import com.example.agent.tools.ops.domain.vo.OpsPipelineStatusVO;
import com.example.agent.tools.ops.domain.vo.OpsServiceHealthVO;
import com.example.agent.tools.ops.domain.vo.OpsTraceSummaryVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 运维诊断 Mock 客户端。
 *
 * <p>当前用于搭建 OpsAgent Tool 调用框架。后续对接真实监控、日志、Trace 或流水线平台时，
 * 新增 HTTP 实现即可保持 Tool 方法签名稳定。</p>
 */
@Component
public class MockOpsDiagnosticClient implements OpsDiagnosticClient {

    @Override
    public OpsServiceHealthVO queryServiceHealth(String serviceName, String env) {
        String normalizedService = normalize(serviceName, "theme-publish-service");
        String normalizedEnv = normalize(env, "prod");

        if (normalizedService.contains("publish") || normalizedService.contains("sync")) {
            return OpsServiceHealthVO.builder()
                    .serviceName(normalizedService)
                    .env(normalizedEnv)
                    .status("DEGRADED")
                    .qps(128.6D)
                    .p95LatencyMs(2860)
                    .errorRate(0.083D)
                    .cpuUsage(0.76D)
                    .memoryUsage(0.68D)
                    .instances(List.of(
                            OpsInstanceStatusVO.builder()
                                    .instanceId(normalizedService + "-pod-7c9f")
                                    .status("UNHEALTHY")
                                    .lastRestartTime("2026-05-23 17:42:11")
                                    .lastError("连接 CDN 回源接口超时")
                                    .build(),
                            OpsInstanceStatusVO.builder()
                                    .instanceId(normalizedService + "-pod-81ab")
                                    .status("HEALTHY")
                                    .lastRestartTime("2026-05-22 09:16:40")
                                    .lastError("")
                                    .build()
                    ))
                    .updatedTime("2026-05-23 18:12:00")
                    .suggestion("优先检查不健康实例的外部依赖超时、CDN 回源接口和最近发布变更。")
                    .build();
        }

        return OpsServiceHealthVO.builder()
                .serviceName(normalizedService)
                .env(normalizedEnv)
                .status("HEALTHY")
                .qps(42.3D)
                .p95LatencyMs(180)
                .errorRate(0.002D)
                .cpuUsage(0.31D)
                .memoryUsage(0.45D)
                .instances(List.of(
                        OpsInstanceStatusVO.builder()
                                .instanceId(normalizedService + "-pod-01")
                                .status("HEALTHY")
                                .lastRestartTime("2026-05-21 03:20:00")
                                .lastError("")
                                .build()
                ))
                .updatedTime("2026-05-23 18:12:00")
                .suggestion("服务当前无明显异常，可结合用户反馈时间继续查询 Trace 或告警。")
                .build();
    }

    @Override
    public OpsAlertSummaryVO queryRecentAlerts(String serviceName, String timeRange) {
        String normalizedService = normalize(serviceName, "theme-publish-service");
        String normalizedTimeRange = normalize(timeRange, "最近30分钟");

        return OpsAlertSummaryVO.builder()
                .serviceName(normalizedService)
                .timeRange(normalizedTimeRange)
                .totalCount(3)
                .highestLevel("P1")
                .status("触发中")
                .alerts(List.of(
                        OpsAlertItemVO.builder()
                                .level("P1")
                                .title("发布接口错误率升高")
                                .status("触发中")
                                .firstSeenTime("2026-05-23 17:48:00")
                                .lastSeenTime("2026-05-23 18:10:00")
                                .description("POST /publish/theme 错误率超过 5%，主要错误为 CDN_SYNC_TIMEOUT。")
                                .build(),
                        OpsAlertItemVO.builder()
                                .level("P2")
                                .title("P95 响应耗时升高")
                                .status("触发中")
                                .firstSeenTime("2026-05-23 17:52:00")
                                .lastSeenTime("2026-05-23 18:09:00")
                                .description("P95 响应耗时持续高于 2 秒。")
                                .build()
                ))
                .suggestion("先确认是否有发布变更，再检查 CDN 同步链路和下游依赖超时。")
                .build();
    }

    @Override
    public OpsPipelineStatusVO queryPipelineStatus(String appName, String env) {
        String normalizedApp = normalize(appName, "theme-publish-service");
        String normalizedEnv = normalize(env, "prod");

        return OpsPipelineStatusVO.builder()
                .appName(normalizedApp)
                .env(normalizedEnv)
                .status("FAILED")
                .pipelineRunId("pipe-20260523-174501")
                .failedStage("资源同步回归检查")
                .reason("CDN 预热校验超时，部分资源 URL 返回 504。")
                .updatedTime("2026-05-23 17:56:20")
                .suggestion("建议先暂停继续发布，确认 CDN 预热任务状态；如为单批资源异常，可重新触发资源同步阶段。")
                .build();
    }

    @Override
    public OpsTraceSummaryVO queryTraceSummary(String traceId) {
        String normalizedTraceId = normalize(traceId, "trace-demo-001");

        return OpsTraceSummaryVO.builder()
                .traceId(normalizedTraceId)
                .entryService("theme-api-gateway")
                .requestPath("POST /api/theme/publish")
                .status("ERROR")
                .totalCostMs(3280)
                .errorSpan("theme-publish-service -> cdn-sync-adapter")
                .errorCode("CDN_SYNC_TIMEOUT")
                .errorMessage("调用 CDN 资源同步接口超时")
                .criticalPath(List.of(
                        "theme-api-gateway",
                        "theme-publish-service",
                        "resource-center",
                        "cdn-sync-adapter"
                ))
                .suggestion("优先排查 cdn-sync-adapter 的下游超时和资源中心返回耗时，必要时按 traceId 继续查看完整日志。")
                .build();
    }

    private String normalize(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
