package com.example.agent.rag.controller;

import com.example.agent.rag.domain.vo.KbDocumentFileVO;
import com.example.agent.rag.domain.vo.KbIngestResultVO;
import com.example.agent.rag.domain.vo.KbSearchResultVO;
import com.example.agent.rag.domain.vo.RagEvaluationReportVO;
import com.example.agent.rag.service.KnowledgeBaseService;
import com.example.agent.rag.service.RagEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库调试和管理接口。
 *
 * <p>当前 RAG 第一版直接使用 {@code docs/rag/theme-business} 目录下的文件作为知识源。
 * 这些接口主要服务于本地调试和管理页面，方便按单个文件重建向量，避免每次都全量导入。
 */
@RestController
@RequestMapping("/admin/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    private final RagEvaluationService ragEvaluationService;

    /**
     * 查询主题业务知识库文件列表，给前端管理台展示文件元信息。
     */
    @GetMapping("/theme-business/files")
    public List<KbDocumentFileVO> listThemeBusinessKnowledgeFiles() {
        return knowledgeBaseService.listThemeBusinessKnowledgeFiles();
    }

    /**
     * 将整个主题业务知识库重新导入 Redis Stack 向量库。
     */
    @PostMapping("/ingest/theme-business")
    public KbIngestResultVO ingestThemeBusinessKnowledge() {
        return knowledgeBaseService.ingestThemeBusinessKnowledge();
    }

    /**
     * 重建单个知识库文件。写入新向量前会先删除该文件旧的切片向量。
     */
    @PostMapping("/ingest/theme-business/file")
    public KbIngestResultVO ingestThemeBusinessKnowledgeFile(@RequestParam String path) {
        return knowledgeBaseService.ingestThemeBusinessKnowledgeFile(path);
    }

    /**
     * 删除单个文件对应的向量数据，不删除本地原始文档。
     */
    @PostMapping("/delete/theme-business/file")
    public KbIngestResultVO deleteThemeBusinessKnowledgeFile(@RequestParam String path) {
        return knowledgeBaseService.deleteThemeBusinessKnowledgeFile(path);
    }

    /**
     * 执行 Redis Stack 相似度检索，用于调试 RAG 命中效果。
     */
    @GetMapping("/search")
    public List<KbSearchResultVO> search(@RequestParam String query) {
        return knowledgeBaseService.search(query);
    }

    /**
     * 基于固定测试集评估 RAG 召回质量，统计 expected 文档是否出现在 TopK 结果中。
     */
    @GetMapping("/evaluate/theme-business")
    public RagEvaluationReportVO evaluateThemeBusinessQueries() {
        return ragEvaluationService.evaluateThemeBusinessQueries();
    }
}
