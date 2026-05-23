package com.example.agent.tools.ops.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 服务健康状态快照。
 */
@Data
@Builder
public class OpsServiceHealthVO {

    /**
     * 服务名称。
     */
    private String serviceName;

    /**
     * 环境，例如 test、pre、prod。
     */
    private String env;

    /**
     * 整体健康状态。
     */
    private String status;

    /**
     * 每秒请求量。
     */
    private Double qps;

    /**
     * P95 响应耗时，单位毫秒。
     */
    private Integer p95LatencyMs;

    /**
     * 错误率。
     */
    private Double errorRate;

    /**
     * CPU 使用率。
     */
    private Double cpuUsage;

    /**
     * 内存使用率。
     */
    private Double memoryUsage;

    /**
     * 实例状态列表。
     */
    private List<OpsInstanceStatusVO> instances;

    /**
     * 最近更新时间。
     */
    private String updatedTime;

    /**
     * 处理建议。
     */
    private String suggestion;
}
