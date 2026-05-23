package com.example.agent.core.client;

import com.example.agent.core.domain.vo.ThemeBusinessSnapshotVO;

/**
 * 主题业务服务客户端抽象。
 *
 * <p>Agent Tool 不直接关心 HTTP 细节，后续接入真实主题业务服务时，
 * 只需要替换这个接口的实现即可。
 */
public interface ThemeBusinessClient {

    /**
     * 查询主题业务快照，包含主题状态、审核状态、上架状态和处理建议。
     *
     * @param themeId 主题 ID
     * @return 主题业务快照
     */
    ThemeBusinessSnapshotVO queryThemeBusinessSnapshot(String themeId);
}
