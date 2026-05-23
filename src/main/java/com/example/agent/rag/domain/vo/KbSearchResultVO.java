package com.example.agent.rag.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class KbSearchResultVO {

    private String id;

    private String content;

    private Double score;

    private Map<String, Object> metadata;
}

