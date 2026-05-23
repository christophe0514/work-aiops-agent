package com.example.agent.core.service.impl;

import com.example.agent.core.config.RagProperties;
import com.example.agent.core.domain.vo.KbDocumentFileVO;
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

/**
 * 基于本地文件的知识库实现，用于 Redis Stack RAG 第一版。
 *
 * <p>当前服务将 {@code docs/rag/theme-business} 作为知识源，将文件切片后写入
 * Spring AI Redis VectorStore。这里先保持轻量，不引入数据库文档管理表，方便本地快速调试。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    /**
     * 主题业务知识库支持导入的源文件格式。
     */
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(
            ".md", ".txt", ".csv", ".tsv", ".json", ".jsonl", ".yaml", ".yml", ".sql"
    );

    private final VectorStore vectorStore;

    private final RagProperties ragProperties;

    @Override
    public List<KbDocumentFileVO> listThemeBusinessKnowledgeFiles() {
        Path basePath = getBasePath();
        if (!Files.exists(basePath)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(basePath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(file -> toFileVO(basePath, file))
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list knowledge base files.", ex);
        }
    }

    @Override
    public KbIngestResultVO ingestThemeBusinessKnowledge() {
        Path basePath = getBasePath();
        if (!Files.exists(basePath)) {
            return KbIngestResultVO.builder()
                    .knowledgeBasePath(basePath.toString())
                    .message("Knowledge base directory does not exist.")
                    .build();
        }

        List<Document> documents = new ArrayList<>();
        int fileCount = 0;

        // 全量导入主要用于首次初始化，日常调试建议优先使用单文件重建。
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

        addDocumentsInBatches(documents);

        return KbIngestResultVO.builder()
                .knowledgeBasePath(basePath.toString())
                .fileCount(fileCount)
                .chunkCount(documents.size())
                .message("Knowledge base ingested into Redis VectorStore.")
                .build();
    }

    @Override
    public KbIngestResultVO ingestThemeBusinessKnowledgeFile(String path) {
        Path basePath = getBasePath();
        Path file = resolveKnowledgeFile(basePath, path);

        if (!Files.exists(file) || !Files.isRegularFile(file) || !isSupportedFile(file)) {
            return KbIngestResultVO.builder()
                    .knowledgeBasePath(basePath.toString())
                    .fileCount(0)
                    .chunkCount(0)
                    .message("Knowledge base file does not exist or is not supported.")
                    .build();
        }

        // 单文件重建前必须先清理旧切片，否则检索时可能命中过期规则。
        deleteBySourcePath(toRelativePath(basePath, file));

        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            List<Document> documents = StringUtils.hasText(content) ? toDocuments(basePath, file, content) : List.of();
            addDocumentsInBatches(documents);
            return KbIngestResultVO.builder()
                    .knowledgeBasePath(basePath.toString())
                    .fileCount(documents.isEmpty() ? 0 : 1)
                    .chunkCount(documents.size())
                    .message("Knowledge base file ingested into Redis VectorStore.")
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to ingest knowledge base file.", ex);
        }
    }

    @Override
    public KbIngestResultVO deleteThemeBusinessKnowledgeFile(String path) {
        Path basePath = getBasePath();
        Path file = resolveKnowledgeFile(basePath, path);
        String relativePath = toRelativePath(basePath, file);
        deleteBySourcePath(relativePath);

        return KbIngestResultVO.builder()
                .knowledgeBasePath(basePath.toString())
                .fileCount(1)
                .chunkCount(0)
                .message("Knowledge base vectors deleted for " + relativePath + ".")
                .build();
    }

    @Override
    public List<KbSearchResultVO> search(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        // 管理台调试检索和 ChatService 回答前检索使用同一套参数，便于排查命中差异。
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
        String relativePath = toRelativePath(basePath, file);
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

            // 使用稳定 ID 可以按文件删除和重建向量，不依赖 Redis 元数据过滤能力。
            String id = stableId(relativePath + "#" + i);
            documents.add(new Document(id, chunks.get(i), metadata));
        }
        return documents;
    }

    private void addDocumentsInBatches(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }

        // DashScope embedding 单批输入不能超过 10，这里对配置值做强制保护。
        int batchSize = Math.max(1, Math.min(ragProperties.getEmbeddingBatchSize(), 10));
        for (int start = 0; start < documents.size(); start += batchSize) {
            int end = Math.min(start + batchSize, documents.size());
            vectorStore.add(documents.subList(start, end));
        }
    }

    private void deleteBySourcePath(String sourcePath) {
        List<String> ids = new ArrayList<>();
        int maxDeleteChunks = Math.max(1, ragProperties.getMaxDeleteChunksPerFile());
        // RedisVectorStore 只有被索引的元数据字段才能过滤删除；这里用稳定 ID 删除更可控。
        for (int i = 0; i < maxDeleteChunks; i++) {
            ids.add(stableId(sourcePath + "#" + i));
        }
        vectorStore.delete(ids);
    }

    private KbDocumentFileVO toFileVO(Path basePath, Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            return KbDocumentFileVO.builder()
                    .path(toRelativePath(basePath, file))
                    .title(resolveTitle(file, content))
                    .fileType(getExtension(file))
                    .size(Files.size(file))
                    .lastModified(Files.getLastModifiedTime(file).toMillis())
                    .chunkCount(splitContent(content).size())
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read knowledge base file metadata.", ex);
        }
    }

    private Path getBasePath() {
        return Path.of(ragProperties.getKnowledgeBasePath()).toAbsolutePath().normalize();
    }

    private Path resolveKnowledgeFile(Path basePath, String path) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("Knowledge file path must not be empty.");
        }

        Path resolved = basePath.resolve(path).normalize();
        // 防止调用方通过 ../../application.yml 这类路径逃逸知识库目录。
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("Knowledge file path is outside base directory.");
        }
        return resolved;
    }

    private String toRelativePath(Path basePath, Path file) {
        return basePath.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private List<String> splitContent(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        int chunkSize = Math.max(300, ragProperties.getChunkSize());
        int overlap = Math.max(0, Math.min(ragProperties.getChunkOverlap(), chunkSize / 2));
        List<String> chunks = new ArrayList<>();

        // 切片大小服务于 embedding，同时尽量优先选择自然文本边界。
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
        // 优先按段落或换行切分，通常比硬截断字符更适合后续检索和引用展示。
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
