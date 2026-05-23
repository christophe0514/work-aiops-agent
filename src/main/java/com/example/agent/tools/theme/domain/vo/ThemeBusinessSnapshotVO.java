package com.example.agent.tools.theme.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 主题业务快照，聚合主题状态、审核状态和上架状态。
 *
 * <p>当前由 Mock 客户端返回，后续对接主题业务服务时保持字段语义稳定即可。
 */
@Data
@Builder
public class ThemeBusinessSnapshotVO {

    /**
     * 主题 ID。
     */
    private String themeId;

    /**
     * 主题名称。
     */
    private String themeName;

    /**
     * 创作者 ID。
     */
    private String creatorId;

    /**
     * 主题主状态，例如草稿、审核中、已上架、已下架。
     */
    private String themeStatus;

    /**
     * 最新审核状态，例如待审核、审核通过、审核驳回。
     */
    private String auditStatus;

    /**
     * 上架状态，例如未上架、上架中、已上架、上架失败。
     */
    private String publishStatus;

    /**
     * 可见渠道，例如 Android 主题商店、搜索页、创作者主页。
     */
    private List<String> visibleChannels;

    /**
     * 当前不可见或失败时的业务原因。
     */
    private String reason;

    /**
     * 建议运营人员下一步处理动作。
     */
    private String suggestion;

    /**
     * 最近一次更新时间。
     */
    private String updatedTime;

    /**
     * 最近审核记录。
     */
    private List<ThemeAuditRecordVO> auditRecords;
}
