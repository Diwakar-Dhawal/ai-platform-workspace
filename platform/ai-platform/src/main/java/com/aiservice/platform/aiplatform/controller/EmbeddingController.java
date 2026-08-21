package com.aiservice.platform.aiplatform.controller;

import com.aiservice.platform.aiplatform.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/embeddings")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @PostMapping("/embed")
    public Map<String, Object> embed(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        log.info("Embed request: {} chars", text != null ? text.length() : 0);
        List<Float> embedding = embeddingService.embed(text);
        log.debug("Embedding returned {} dimensions", embedding.size());
        return Map.of("embedding", embedding, "dimensions", embedding.size());
    }

    @PostMapping("/embed-batch")
    public Map<String, Object> embedBatch(@RequestBody Map<String, List<String>> request) {
        List<String> texts = request.get("texts");
        log.info("Batch embed request: {} texts", texts != null ? texts.size() : 0);
        List<List<Float>> embeddings = embeddingService.embedBatch(texts);
        log.debug("Batch embedding returned {} vectors", embeddings.size());
        return Map.of("embeddings", embeddings, "count", embeddings.size());
    }
}
