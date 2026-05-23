package com.example.agent.core.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 前端知识库管理台展示的本地知识文件元信息。
 */
@Data
@Builder
public class KbDocumentFileVO {

    /**
     * 知识库目录下的相对路径。
     */
    private String path;

    /**
     * 展示标题，优先从 Markdown 一级标题解析，其他格式使用文件名。
     */
    private String title;

    /**
     * 文件扩展名，例如 .md、.json、.csv。
     */
    private String fileType;

    /**
     * 文件大小，单位字节。
     */
    private long size;

    /**
     * 最后修改时间戳，单位毫秒。
     */
    private long lastModified;

    /**
     * 按当前切片配置预估生成的切片数量。
     */
    private int chunkCount;
}
