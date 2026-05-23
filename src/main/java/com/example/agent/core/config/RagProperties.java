package com.example.agent.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aiops.rag")
public class RagProperties {

    private String knowledgeBasePath = "docs/rag/theme-business";

    private String bizDomain = "theme-business";

    private String fallbackOwnerEmployeeNo = "THEME_OPS_OWNER";

    private int topK = 5;

    private double similarityThreshold = 0.65;

    private int chunkSize = 900;

    private int chunkOverlap = 120;
}
