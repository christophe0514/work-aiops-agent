package com.example.agent.core.client.impl;

import com.example.agent.core.client.ThemeBusinessClient;
import com.example.agent.core.domain.vo.ThemeAuditRecordVO;
import com.example.agent.core.domain.vo.ThemeBusinessSnapshotVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主题业务服务 Mock 实现。
 *
 * <p>当前用于搭建 Tool 调用框架和前后端联调。后续对接真实 HTTP 接口时，
 * 可以新增 HTTP 实现并通过配置切换，或直接替换该实现。
 */
@Component
public class MockThemeBusinessClient implements ThemeBusinessClient {

    @Override
    public ThemeBusinessSnapshotVO queryThemeBusinessSnapshot(String themeId) {
        String normalizedThemeId = themeId == null ? "" : themeId.trim();

        if (normalizedThemeId.endsWith("404")) {
            return notFoundSnapshot(normalizedThemeId);
        }
        if (normalizedThemeId.endsWith("2")) {
            return rejectedSnapshot(normalizedThemeId);
        }
        if (normalizedThemeId.endsWith("3")) {
            return publishFailedSnapshot(normalizedThemeId);
        }
        return publishedSnapshot(normalizedThemeId);
    }

    private ThemeBusinessSnapshotVO publishedSnapshot(String themeId) {
        return ThemeBusinessSnapshotVO.builder()
                .themeId(themeId)
                .themeName("星河漫游动态主题")
                .creatorId("creator_10086")
                .themeStatus("已上架")
                .auditStatus("审核通过")
                .publishStatus("已上架")
                .visibleChannels(List.of("Android 主题商店", "搜索页", "创作者主页"))
                .reason("主题已完成审核和资源同步，当前处于正常售卖状态。")
                .suggestion("如运营侧仍反馈不可见，请继续确认用户机型、渠道灰度和缓存刷新时间。")
                .updatedTime("2026-05-23 14:30:00")
                .auditRecords(List.of(
                        ThemeAuditRecordVO.builder()
                                .auditNode("人工复审")
                                .auditResult("通过")
                                .reason("内容、版权、资源规格均符合上架要求。")
                                .operator("audit_zhangsan")
                                .auditTime("2026-05-23 13:55:00")
                                .build()
                ))
                .build();
    }

    private ThemeBusinessSnapshotVO rejectedSnapshot(String themeId) {
        return ThemeBusinessSnapshotVO.builder()
                .themeId(themeId)
                .themeName("霓虹机械桌面主题")
                .creatorId("creator_20002")
                .themeStatus("审核驳回")
                .auditStatus("审核驳回")
                .publishStatus("未上架")
                .visibleChannels(List.of())
                .reason("锁屏预览图与实际资源不一致，且详情页素材存在第三方品牌露出。")
                .suggestion("建议运营通知创作者修改预览图和详情页素材后重新提交审核。")
                .updatedTime("2026-05-22 18:12:00")
                .auditRecords(List.of(
                        ThemeAuditRecordVO.builder()
                                .auditNode("人工初审")
                                .auditResult("驳回")
                                .reason("预览图与实际效果不一致，素材疑似存在品牌侵权风险。")
                                .operator("audit_lisi")
                                .auditTime("2026-05-22 18:10:00")
                                .build()
                ))
                .build();
    }

    private ThemeBusinessSnapshotVO publishFailedSnapshot(String themeId) {
        return ThemeBusinessSnapshotVO.builder()
                .themeId(themeId)
                .themeName("极简晴空图标主题")
                .creatorId("creator_30003")
                .themeStatus("待上架")
                .auditStatus("审核通过")
                .publishStatus("上架失败")
                .visibleChannels(List.of())
                .reason("资源包已审核通过，但 CDN 资源同步超时，前台暂不可见。")
                .suggestion("建议转交 OpsAgent 排查资源同步任务、CDN 回源和发布流水线状态。")
                .updatedTime("2026-05-23 10:42:00")
                .auditRecords(List.of(
                        ThemeAuditRecordVO.builder()
                                .auditNode("人工复审")
                                .auditResult("通过")
                                .reason("审核通过，等待资源同步。")
                                .operator("audit_wangwu")
                                .auditTime("2026-05-23 10:15:00")
                                .build()
                ))
                .build();
    }

    private ThemeBusinessSnapshotVO notFoundSnapshot(String themeId) {
        return ThemeBusinessSnapshotVO.builder()
                .themeId(themeId)
                .themeName("未知主题")
                .themeStatus("未查询到")
                .auditStatus("未查询到")
                .publishStatus("未查询到")
                .visibleChannels(List.of())
                .reason("未查询到该主题 ID 对应的业务记录。")
                .suggestion("请确认主题 ID 是否正确；如果确认无误，建议提交工单给主题业务开发组核查。")
                .updatedTime("2026-05-23 00:00:00")
                .auditRecords(List.of())
                .build();
    }
}
