package com.example.agent.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.rag")
public class RagProperties {

    /**
     * 本地知识库目录，当前阶段直接作为 RAG 知识源。
     */
    private String knowledgeBasePath = "docs/rag/theme-business";

    /**
     * 写入向量元数据的业务域，便于后续过滤和追踪来源。
     */
    private String bizDomain = "theme-business";

    /**
     * 当知识库没有可靠命中时，提示用户联系的兜底业务接口人。
     */
    private String fallbackOwnerEmployeeNo = "THEME_OPS_OWNER";

    /**
     * 单次问题检索返回的候选知识片段数量。
     */
    private int topK = 5;

    /**
     * Spring AI VectorStore 接受的最低相似度阈值。
     */
    private double similarityThreshold = 0.65;

    /**
     * 目标切片字符数。实际切片时会优先选择段落或换行边界。
     */
    private int chunkSize = 900;

    /**
     * 相邻切片之间的重叠字符数，用于减少边界处的上下文丢失。
     */
    private int chunkOverlap = 120;

    /**
     * DashScope embedding 单批输入数量不能超过 10。
     */
    private int embeddingBatchSize = 10;

    /**
     * 重建单个源文件时，最多尝试删除的旧向量切片数量。
     */
    private int maxDeleteChunksPerFile = 2000;
}
