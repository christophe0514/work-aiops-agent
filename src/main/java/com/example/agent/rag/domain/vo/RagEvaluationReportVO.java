package com.example.agent.rag.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RagEvaluationReportVO {

    private String datasetPath;

    private Integer total;

    private Integer hitCount;

    private Integer missCount;

    private Double hitRate;

    private Double averageBestScore;

    private List<RagEvaluationCaseVO> cases;
}
