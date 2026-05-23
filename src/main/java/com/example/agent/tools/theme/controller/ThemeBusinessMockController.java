package com.example.agent.tools.theme.controller;

import com.example.agent.tools.theme.client.ThemeBusinessClient;
import com.example.agent.tools.theme.domain.vo.ThemeBusinessSnapshotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 主题业务 Mock 调试接口。
 *
 * <p>该接口不参与 Agent Tool 调用，只用于开发阶段直接查看模拟业务数据结构。
 * 后续接入真实主题业务服务后，可以删除或迁移到测试环境专用接口。
 */
@RestController
@RequestMapping("/mock/theme-business")
@RequiredArgsConstructor
public class ThemeBusinessMockController {

    private final ThemeBusinessClient themeBusinessClient;

    /**
     * 直接查询模拟主题业务快照，便于确认 Tool 底层返回字段是否符合预期。
     */
    @GetMapping("/snapshot")
    public ThemeBusinessSnapshotVO queryThemeBusinessSnapshot(@RequestParam String themeId) {
        return themeBusinessClient.queryThemeBusinessSnapshot(themeId);
    }
}
