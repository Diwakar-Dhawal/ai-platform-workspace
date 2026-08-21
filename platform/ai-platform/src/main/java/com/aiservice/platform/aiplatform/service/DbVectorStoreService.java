package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.model.Chunk;
import com.aiservice.platform.aiplatform.model.ChunkEmbedding;
import com.aiservice.platform.aiplatform.model.SearchResult;
import com.aiservice.platform.aiplatform.repository.ChunkEmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Database-backed vector store using PostgreSQL.
 * Stores embeddings in a table and performs cosine similarity search in Java.
 *
 * Activated when Pinecone is NOT configured (ai-platform.vector-store=db).
 * Suitable for MVP scale (<100K vectors). For production, use Pinecone.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai-platform.vector-store", havingValue = "db", matchIfMissing = true)
public class DbVectorStoreService implements VectorStoreService {

    private final ChunkEmbeddingRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void store(Chunk chunk, List<Float> embedding) {
        try {
            Float[] embeddingArray = embedding.toArray(new Float[0]);
            String metadataJson = chunk.getMetadata() != null
                    ? objectMapper.writeValueAsString(chunk.getMetadata())
                    : "{}";

            ChunkEmbedding entity = ChunkEmbedding.builder()
                    .id(chunk.getId())
                    .sessionId(chunk.getSessionId())
                    .appId(chunk.getAppId())
                    .content(chunk.getContent())
                    .chunkIndex(chunk.getChunkIndex())
                    .totalChunks(chunk.getTotalChunks())
                    .embedding(embeddingArray)
                    .metadataJson(metadataJson)
                    .build();

            repository.save(entity);
            log.debug("Stored chunk {} in DB (session={}, app={})", chunk.getId(), chunk.getSessionId(), chunk.getAppId());

        } catch (Exception e) {
            log.error("Failed to store chunk {} in DB: {}", chunk.getId(), e.getMessage());
            throw new RuntimeException("DB vector store failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void storeBatch(List<ChunkWithEmbedding> chunks) {
        try {
            List<ChunkEmbedding> entities = chunks.stream().map(item -> {
                Chunk chunk = item.chunk();
                List<Float> embedding = item.embedding();
                try {
                    String metadataJson = chunk.getMetadata() != null
                            ? objectMapper.writeValueAsString(chunk.getMetadata())
                            : "{}";

                    return ChunkEmbedding.builder()
                            .id(chunk.getId())
                            .sessionId(chunk.getSessionId())
                            .appId(chunk.getAppId())
                            .content(chunk.getContent())
                            .chunkIndex(chunk.getChunkIndex())
                            .totalChunks(chunk.getTotalChunks())
                            .embedding(embedding.toArray(new Float[0]))
                            .metadataJson(metadataJson)
                            .build();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize metadata", e);
                }
            }).toList();

            repository.saveAll(entities);
            log.info("Batch stored {} chunks in DB", entities.size());

        } catch (Exception e) {
            log.error("Batch store failed: {}", e.getMessage());
            throw new RuntimeException("DB batch store failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SearchResult> search(
            List<Float> queryEmbedding,
            String appId,
            int topK,
            double similarityThreshold
    ) {
        List<ChunkEmbedding> candidates = repository.findByAppId(appId);

        if (candidates.isEmpty()) {
            log.debug("No chunks found for app '{}' in DB", appId);
            return List.of();
        }

        List<SearchResult> results = candidates.stream()
                .map(entity -> {
                    double score = cosineSimilarity(queryEmbedding, toFloatList(entity.getEmbedding()));
                    Chunk chunk = toChunk(entity);
                    String citation = buildCitation(entity);
                    return SearchResult.builder()
                            .chunk(chunk)
                            .score(score)
                            .citation(citation)
                            .build();
                })
                .filter(r -> r.getScore() >= similarityThreshold)
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(topK)
                .toList();

        log.info("DB search for app '{}': {} candidates, {} results above threshold {}",
                appId, candidates.size(), results.size(), similarityThreshold);

        return results;
    }

    @Override
    public List<SearchResult> searchWithinSession(
            List<Float> queryEmbedding,
            UUID contentSessionId,
            int topK,
            double similarityThreshold
    ) {
        List<ChunkEmbedding> candidates = repository.findBySessionId(contentSessionId);

        if (candidates.isEmpty()) {
            log.debug("No chunks found for session {} in DB", contentSessionId);
            return List.of();
        }

        List<SearchResult> results = candidates.stream()
                .map(entity -> {
                    double score = cosineSimilarity(queryEmbedding, toFloatList(entity.getEmbedding()));
                    Chunk chunk = toChunk(entity);
                    String citation = buildCitation(entity);
                    return SearchResult.builder()
                            .chunk(chunk)
                            .score(score)
                            .citation(citation)
                            .build();
                })
                .filter(r -> r.getScore() >= similarityThreshold)
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .limit(topK)
                .toList();

        log.info("DB search for session {}: {} candidates, {} results (threshold={})",
                contentSessionId, candidates.size(), results.size(), similarityThreshold);

        return results;
    }

    @Override
    public void deleteSession(UUID contentSessionId) {
        repository.deleteBySessionId(contentSessionId);
        log.info("Deleted all chunks for session {} from DB", contentSessionId);
    }

    @Override
    public void deleteApp(String appId) {
        repository.deleteByAppId(appId);
        log.info("Deleted all chunks for app '{}' from DB", appId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private Chunk toChunk(ChunkEmbedding entity) {
        Map<String, Object> metadata = new HashMap<>();
        if (entity.getMetadataJson() != null && !entity.getMetadataJson().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(entity.getMetadataJson(), Map.class);
                metadata = parsed;
            } catch (Exception e) {
                log.debug("Failed to parse metadata JSON for chunk {}", entity.getId());
            }
        }

        return Chunk.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .appId(entity.getAppId())
                .content(entity.getContent())
                .chunkIndex(entity.getChunkIndex())
                .totalChunks(entity.getTotalChunks())
                .metadata(metadata)
                .build();
    }

    private String buildCitation(ChunkEmbedding entity) {
        Map<String, Object> meta = new HashMap<>();
        if (entity.getMetadataJson() != null && !entity.getMetadataJson().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(entity.getMetadataJson(), Map.class);
                meta = parsed;
            } catch (Exception ignored) {}
        }

        if (meta.containsKey("timestamp")) {
            return "Video at " + meta.get("timestamp");
        }
        if (meta.containsKey("page_number")) {
            return "Page " + meta.get("page_number");
        }
        if (meta.containsKey("section_title")) {
            return String.valueOf(meta.get("section_title"));
        }
        if (meta.containsKey("videoTitle")) {
            return String.valueOf(meta.get("videoTitle"));
        }
        return "Chunk " + entity.getChunkIndex();
    }

    private List<Float> toFloatList(Float[] array) {
        if (array == null) return List.of();
        return Arrays.asList(array);
    }

    /**
     * Compute cosine similarity between two vectors.
     * Both should have the same dimension (3072 for Gemini text-embedding-004).
     */
    static double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.size() != b.size() || a.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            double va = a.get(i);
            double vb = b.get(i);
            dotProduct += va * vb;
            normA += va * va;
            normB += vb * vb;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
