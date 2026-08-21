package com.aiservice.platform.aiplatform.repository;

import com.aiservice.platform.aiplatform.model.ChunkEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbedding, UUID> {

    List<ChunkEmbedding> findByAppId(String appId);

    List<ChunkEmbedding> findBySessionId(UUID sessionId);

    List<ChunkEmbedding> findByAppIdAndSessionId(String appId, UUID sessionId);

    void deleteBySessionId(UUID sessionId);

    void deleteByAppId(String appId);

    @Query(value = """
            SELECT * FROM chunk_embeddings 
            WHERE app_id = :appId 
            LIMIT :limit
            """, nativeQuery = true)
    List<ChunkEmbedding> findSampleByAppId(@Param("appId") String appId, @Param("limit") int limit);
}
