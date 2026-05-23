package com.example.agent.rag.service;

import com.example.agent.rag.domain.vo.KbDocumentFileVO;
import com.example.agent.rag.domain.vo.KbIngestResultVO;
import com.example.agent.rag.domain.vo.KbSearchResultVO;

import java.util.List;

/**
 * 主题业务知识库服务，同时供管理台和 Agent RAG 流程使用。
 */
public interface KnowledgeBaseService {

    /**
     * 读取本地知识库文件，并返回前端展示所需的元信息。
     */
    List<KbDocumentFileVO> listThemeBusinessKnowledgeFiles();

    /**
     * 导入配置目录下所有支持格式的知识库文件。
     */
    KbIngestResultVO ingestThemeBusinessKnowledge();

    /**
     * 重建单个文件的向量数据，这是日常调试 RAG 时最常用的入口。
     */
    KbIngestResultVO ingestThemeBusinessKnowledgeFile(String path);

    /**
     * 删除单个文件的向量数据，但保留本地知识库原文。
     */
    KbIngestResultVO deleteThemeBusinessKnowledgeFile(String path);

    /**
     * 检索 Redis VectorStore，返回命中的知识片段和元数据，用于支撑 Agent 回答。
     */
    List<KbSearchResultVO> search(String query);
}
