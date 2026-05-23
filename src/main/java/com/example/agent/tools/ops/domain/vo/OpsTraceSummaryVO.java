package com.example.agent.tools.ops.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Trace 调用链摘要。
 */
@Data
@Builder
public class OpsTraceSummaryVO {

    /**
     * Trace ID。
     */
    private String traceId;

    /**
     * 入口服务。
     */
    private String entryService;

    /**
     * 请求路径。
     */
    private String requestPath;

    /**
     * 调用链状态。
     */
    private String status;

    /**
     * 总耗时，单位毫秒。
     */
    private Integer totalCostMs;

    /**
     * 异常节点。
     */
    private String errorSpan;

    /**
     * 错误码。
     */
    private String errorCode;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 关键调用路径。
     */
    private List<String> criticalPath;

    /**
     * 处理建议。
     */
    private String suggestion;
}
