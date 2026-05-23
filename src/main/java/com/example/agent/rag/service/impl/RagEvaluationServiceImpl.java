package com.example.agent.rag.service.impl;

import com.example.agent.rag.config.RagProperties;
import com.example.agent.rag.domain.vo.KbSearchResultVO;
import com.example.agent.rag.domain.vo.RagEvaluationCaseVO;
import com.example.agent.rag.domain.vo.RagEvaluationReportVO;
import com.example.agent.rag.service.KnowledgeBaseService;
import com.example.agent.rag.service.RagEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RagEvaluationServiceImpl implements RagEvaluationService {

    private static final String DEFAULT_DATASET = "data/search_test_queries.tsv";

    private final KnowledgeBaseService knowledgeBaseService;

    private final RagProperties ragProperties;

    @Override
    public RagEvaluationReportVO evaluateThemeBusinessQueries() {
        Path datasetPath = Path.of(ragProperties.getKnowledgeBasePath()).resolve(DEFAULT_DATASET).toAbsolutePath().normalize();
        List<TestCase> testCases = loadDataset(datasetPath);
        List<RagEvaluationCaseVO> caseReports = new ArrayList<>();

        for (TestCase testCase : testCases) {
            List<KbSearchResultVO> results = knowledgeBaseService.search(testCase.query());
            Integer hitRank = findHitRank(results, testCase.expectedPrimaryDoc());
            KbSearchResultVO best = results.isEmpty() ? null : results.get(0);
            caseReports.add(RagEvaluationCaseVO.builder()
                    .query(testCase.query())
                    .expectedPrimaryDoc(testCase.expectedPrimaryDoc())
                    .expectedIntent(testCase.expectedIntent())
                    .notes(testCase.notes())
                    .hit(hitRank != null)
                    .hitRank(hitRank)
                    .bestScore(best == null ? null : best.getScore())
                    .bestMatchedDoc(best == null ? null : sourcePath(best))
                    .results(results)
                    .build());
        }

        int total = caseReports.size();
        int hitCount = (int) caseReports.stream().filter(item -> Boolean.TRUE.equals(item.getHit())).count();
        double averageBestScore = caseReports.stream()
                .map(RagEvaluationCaseVO::getBestScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0D);

        return RagEvaluationReportVO.builder()
                .datasetPath(datasetPath.toString())
                .total(total)
                .hitCount(hitCount)
                .missCount(total - hitCount)
                .hitRate(total == 0 ? 0.0D : hitCount * 1.0D / total)
                .averageBestScore(averageBestScore)
                .cases(caseReports)
                .build();
    }

    private List<TestCase> loadDataset(Path datasetPath) {
        if (!Files.exists(datasetPath)) {
            throw new IllegalStateException("RAG evaluation dataset does not exist: " + datasetPath);
        }

        try {
            List<String> lines = Files.readAllLines(datasetPath, StandardCharsets.UTF_8);
            List<TestCase> testCases = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                if (columns.length < 2) {
                    continue;
                }
                testCases.add(new TestCase(
                        columns[0].trim(),
                        columns[1].trim(),
                        columns.length > 2 ? columns[2].trim() : "",
                        columns.length > 3 ? columns[3].trim() : ""
                ));
            }
            return testCases;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load RAG evaluation dataset: " + datasetPath, ex);
        }
    }

    private Integer findHitRank(List<KbSearchResultVO> results, String expectedPrimaryDoc) {
        if (!StringUtils.hasText(expectedPrimaryDoc)) {
            return null;
        }
        for (int i = 0; i < results.size(); i++) {
            String sourcePath = sourcePath(results.get(i));
            if (sourcePath != null && sourcePath.endsWith(expectedPrimaryDoc)) {
                return i + 1;
            }
        }
        return null;
    }

    private String sourcePath(KbSearchResultVO result) {
        if (result.getMetadata() == null) {
            return null;
        }
        Object sourcePath = result.getMetadata().get("sourcePath");
        return sourcePath == null ? null : String.valueOf(sourcePath);
    }

    private record TestCase(String query, String expectedPrimaryDoc, String expectedIntent, String notes) {
    }
}
