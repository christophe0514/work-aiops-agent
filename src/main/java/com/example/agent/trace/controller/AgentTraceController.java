package com.example.agent.trace.controller;

import com.example.agent.trace.domain.vo.AgentTraceLogVO;
import com.example.agent.trace.service.AgentTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/agent-traces")
@RequiredArgsConstructor
public class AgentTraceController {

    private final AgentTraceService agentTraceService;

    @GetMapping("/{traceId}")
    public List<AgentTraceLogVO> listByTraceId(@PathVariable String traceId) {
        return agentTraceService.listByTraceId(traceId);
    }
}
