package com.example.agent.tools.ops.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 服务实例状态。
 */
@Data
@Builder
public class OpsInstanceStatusVO {

    /**
     * 实例 ID。
     */
    private String instanceId;

    /**
     * 实例状态。
     */
    private String status;

    /**
     * 最近重启时间。
     */
    private String lastRestartTime;

    /**
     * 最近错误说明。
     */
    private String lastError;
}
