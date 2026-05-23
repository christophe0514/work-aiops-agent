package com.example.agent.core.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 主题审核记录摘要，用于 Tool 返回给大模型组织运营侧答复。
 */
@Data
@Builder
public class ThemeAuditRecordVO {

    /**
     * 审核节点，例如机审、人工初审、人工复审。
     */
    private String auditNode;

    /**
     * 审核结论，例如通过、驳回、待审核。
     */
    private String auditResult;

    /**
     * 驳回或通过原因说明。
     */
    private String reason;

    /**
     * 审核人或审核系统标识。
     */
    private String operator;

    /**
     * 审核发生时间。
     */
    private String auditTime;
}
