package com.example.agent.tools.ops.controller;

import com.example.agent.tools.ops.client.OpsDiagnosticClient;
import com.example.agent.tools.ops.domain.vo.OpsAlertSummaryVO;
import com.example.agent.tools.ops.domain.vo.OpsPipelineStatusVO;
import com.example.agent.tools.ops.domain.vo.OpsServiceHealthVO;
import com.example.agent.tools.ops.domain.vo.OpsTraceSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维诊断 Mock 调试接口。
 *
 * <p>这些接口不参与 Agent Tool 调用，只用于开发阶段直接查看模拟诊断数据结构。</p>
 */
@RestController
@RequestMapping("/mock/ops")
@RequiredArgsConstructor
public class OpsDiagnosticMockController {

    private final OpsDiagnosticClient opsDiagnosticClient;

    @GetMapping("/service-health")
    public OpsServiceHealthVO queryServiceHealth(@RequestParam(defaultValue = "theme-publish-service") String serviceName,
                                                 @RequestParam(defaultValue = "prod") String env) {
        return opsDiagnosticClient.queryServiceHealth(serviceName, env);
    }

    @GetMapping("/alerts")
    public OpsAlertSummaryVO queryRecentAlerts(@RequestParam(defaultValue = "theme-publish-service") String serviceName,
                                               @RequestParam(defaultValue = "最近30分钟") String timeRange) {
        return opsDiagnosticClient.queryRecentAlerts(serviceName, timeRange);
    }

    @GetMapping("/pipeline")
    public OpsPipelineStatusVO queryPipelineStatus(@RequestParam(defaultValue = "theme-publish-service") String appName,
                                                   @RequestParam(defaultValue = "prod") String env) {
        return opsDiagnosticClient.queryPipelineStatus(appName, env);
    }

    @GetMapping("/trace")
    public OpsTraceSummaryVO queryTraceSummary(@RequestParam(defaultValue = "trace-demo-001") String traceId) {
        return opsDiagnosticClient.queryTraceSummary(traceId);
    }
}
