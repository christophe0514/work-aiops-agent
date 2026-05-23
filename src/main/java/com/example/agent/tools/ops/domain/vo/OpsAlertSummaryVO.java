package com.example.agent.tools.ops.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 服务告警聚合信息。
 */
@Data
@Builder
public class OpsAlertSummaryVO {

    /**
     * 服务名称。
     */
    private String serviceName;

    /**
     * 查询时间范围。
     */
    private String timeRange;

    /**
     * 告警总数。
     */
    private Integer totalCount;

    /**
     * 最高告警级别。
     */
    private String highestLevel;

    /**
     * 当前整体状态。
     */
    private String status;

    /**
     * 告警列表。
     */
    private List<OpsAlertItemVO> alerts;

    /**
     * 处理建议。
     */
    private String suggestion;
}
