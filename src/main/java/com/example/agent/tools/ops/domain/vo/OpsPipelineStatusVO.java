package com.example.agent.tools.ops.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 发布流水线状态。
 */
@Data
@Builder
public class OpsPipelineStatusVO {

    /**
     * 应用名称。
     */
    private String appName;

    /**
     * 环境，例如 test、pre、prod。
     */
    private String env;

    /**
     * 流水线状态。
     */
    private String status;

    /**
     * 最近一次执行 ID。
     */
    private String pipelineRunId;

    /**
     * 当前卡住或失败的阶段。
     */
    private String failedStage;

    /**
     * 失败原因。
     */
    private String reason;

    /**
     * 最近更新时间。
     */
    private String updatedTime;

    /**
     * 处理建议。
     */
    private String suggestion;
}
