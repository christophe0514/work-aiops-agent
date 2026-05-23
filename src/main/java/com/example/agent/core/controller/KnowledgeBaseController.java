package com.example.agent.core.controller;

import com.example.agent.core.domain.vo.KbIngestResultVO;
import com.example.agent.core.domain.vo.KbSearchResultVO;
import com.example.agent.core.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping("/ingest/theme-business")
    public KbIngestResultVO ingestThemeBusinessKnowledge() {
        return knowledgeBaseService.ingestThemeBusinessKnowledge();
    }

    @GetMapping("/search")
    public List<KbSearchResultVO> search(@RequestParam String query) {
        return knowledgeBaseService.search(query);
    }
}
