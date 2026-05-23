package com.example.agent.rag.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagEvaluationCaseVO {

    private String query;

    private String expectedPrimaryDoc;

    private String expectedIntent;

    private String notes;

    private Boolean hit;

    private Integer hitRank;

    private Double bestScore;

    private String bestMatchedDoc;

    private List<KbSearchResultVO> results;
}
