package com.example.agent.tools.ops.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 单条告警摘要。
 */
@Data
@Builder
public class OpsAlertItemVO {

    /**
     * 告警级别，例如 P0、P1、P2。
     */
    private String level;

    /**
     * 告警标题。
     */
    private String title;

    /**
     * 告警状态，例如触发中、已恢复。
     */
    private String status;

    /**
     * 首次触发时间。
     */
    private String firstSeenTime;

    /**
     * 最近一次触发时间。
     */
    private String lastSeenTime;

    /**
     * 告警说明。
     */
    private String description;
}
