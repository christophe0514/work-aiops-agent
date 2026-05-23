package com.example.agent.core.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KbIngestResultVO {

    private String knowledgeBasePath;

    private int fileCount;

    private int chunkCount;

    private String message;
}
