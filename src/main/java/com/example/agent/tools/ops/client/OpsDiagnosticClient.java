package com.example.agent.tools.ops.client;

import com.example.agent.tools.ops.domain.vo.OpsAlertSummaryVO;
import com.example.agent.tools.ops.domain.vo.OpsPipelineStatusVO;
import com.example.agent.tools.ops.domain.vo.OpsServiceHealthVO;
import com.example.agent.tools.ops.domain.vo.OpsTraceSummaryVO;

/**
 * 运维诊断数据客户端。
 *
 * <p>OpsAgent Tool 不直接关心日志平台、监控平台或流水线平台的协议细节，
 * 后续接入真实 HTTP 服务时，只需要替换该接口实现。</p>
 */
public interface OpsDiagnosticClient {

    OpsServiceHealthVO queryServiceHealth(String serviceName, String env);

    OpsAlertSummaryVO queryRecentAlerts(String serviceName, String timeRange);

    OpsPipelineStatusVO queryPipelineStatus(String appName, String env);

    OpsTraceSummaryVO queryTraceSummary(String traceId);
}
