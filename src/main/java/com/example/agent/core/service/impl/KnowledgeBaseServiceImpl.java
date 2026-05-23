package com.example.agent.core.service.impl;

import com.example.agent.core.config.RagProperties;
import com.example.agent.core.domain.vo.KbIngestResultVO;
import com.example.agent.core.domain.vo.KbSearchResultVO;
import com.example.agent.core.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".md", ".txt", ".csv", ".tsv", ".json", ".jsonl", ".yaml", ".yml", ".sql"
    );

    private final VectorStore vectorStore;

    private final RagProperties ragProperties;

    @Override
    public KbIngestResultVO ingestThemeBusinessKnowledge() {
        Path basePath = Path.of(ragProperties.getKnowledgeBasePath()).toAbsolutePath().normalize();
        if (!Files.exists(basePath)) {
            return KbIngestResultVO.builder()
                    .knowledgeBasePath(basePath.toString())
                    .message("Knowledge base directory does not exist.")
                    .build();
        }

        List<Document> documents = new ArrayList<>();
        int fileCount = 0;

        try (Stream<Path> paths = Files.walk(basePath)) {
            List<Path> files = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            for (Path file : files) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (!StringUtils.hasText(content)) {
                    continue;
                }
                fileCount++;
                documents.addAll(toDocuments(basePath, file, content));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to ingest knowledge base files.", ex);
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        return KbIngestResultVO.builder()
                .knowledgeBasePath(basePath.toString())
                .fileCount(fileCount)
                .chunkCount(documents.size())
                .message("Knowledge base ingested into Redis VectorStore.")
                .build();
    }

    @Override
    public List<KbSearchResultVO> search(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(ragProperties.getTopK())
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(document -> KbSearchResultVO.builder()
                        .id(document.getId())
                        .content(document.getText())
                        .score(document.getScore())
                        .metadata(document.getMetadata())
                        .build())
                .toList();
    }

    private List<Document> toDocuments(Path basePath, Path file, String content) {
        String relativePath = basePath.relativize(file).toString().replace('\\', '/');
        String title = resolveTitle(file, content);
        List<String> chunks = splitContent(content);
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("bizDomain", ragProperties.getBizDomain());
            metadata.put("sourcePath", relativePath);
            metadata.put("title", title);
            metadata.put("chunkIndex", i);
            metadata.put("ownerEmployeeNo", ragProperties.getFallbackOwnerEmployeeNo());
            metadata.put("fileType", getExtension(file));

            String id = stableId(relativePath + "#" + i);
            documents.add(new Document(id, chunks.get(i), metadata));
        }
        return documents;
    }

    private List<String> splitContent(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        int chunkSize = Math.max(300, ragProperties.getChunkSize());
        int overlap = Math.max(0, Math.min(ragProperties.getChunkOverlap(), chunkSize / 2));
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < normalized.length()) {
            int maxEnd = Math.min(normalized.length(), start + chunkSize);
            int end = findPreferredEnd(normalized, start, maxEnd);
            String chunk = normalized.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private int findPreferredEnd(String content, int start, int maxEnd) {
        if (maxEnd >= content.length()) {
            return content.length();
        }
        int paragraphEnd = content.lastIndexOf("\n\n", maxEnd);
        if (paragraphEnd > start + 200) {
            return paragraphEnd;
        }
        int lineEnd = content.lastIndexOf('\n', maxEnd);
        if (lineEnd > start + 200) {
            return lineEnd;
        }
        return maxEnd;
    }

    private String resolveTitle(Path file, String content) {
        if (getExtension(file).equals(".md")) {
            return content.lines()
                    .map(String::trim)
                    .filter(line -> line.startsWith("# "))
                    .map(line -> line.substring(2).trim())
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElseGet(() -> stripExtension(file.getFileName().toString()));
        }
        return stripExtension(file.getFileName().toString());
    }

    private boolean isSupportedFile(Path file) {
        return SUPPORTED_EXTENSIONS.contains(getExtension(file));
    }

    private String getExtension(Path file) {
        String fileName = file.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? fileName : fileName.substring(0, index);
    }

    private String stableId(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return "theme-kb-" + HexFormat.of().formatHex(bytes, 0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }
}
