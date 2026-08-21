package com.aiservice.applications.insighttube.tubeservice.controller;

import com.aiservice.applications.insighttube.tubeservice.dto.IngestProgress;
import com.aiservice.applications.insighttube.tubeservice.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server-Sent Events controller for real-time ingest progress streaming.
 * Frontend can subscribe to this for instant progress updates (no polling needed).
 *
 * Usage: GET /api/v1/content/sessions/{id}/stream
 * Returns: SSE stream with IngestProgress events every 1 second.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Tag(name = "Ingest Stream", description = "Real-time ingest progress via Server-Sent Events")
public class IngestSseController {

    private final IngestionService ingestionService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream ingest progress (SSE)",
            description = "Real-time progress updates via Server-Sent Events. " +
                    "Subscribe for instant updates instead of polling."
    )
    public SseEmitter streamIngestProgress(@PathVariable UUID sessionId) {
        log.info("SSE client connected for session {}", sessionId);

        SseEmitter emitter = new SseEmitter(300_000L); // 5 minute timeout

        executor.execute(() -> {
            try {
                while (true) {
                    IngestProgress progress = ingestionService.getProgress(sessionId);

                    if (progress == null) {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(Map.of("error", "Session not found")));
                        emitter.complete();
                        return;
                    }

                    // Send progress event
                    emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(progress));

                    // If completed or failed, send final event and close
                    if ("COMPLETED".equals(progress.getStatus()) || "FAILED".equals(progress.getStatus())) {
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(progress));
                        emitter.complete();
                        log.info("SSE stream completed for session {} (status: {})",
                                sessionId, progress.getStatus());
                        return;
                    }

                    // Wait 1 second before next update
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.warn("SSE stream interrupted for session {}: {}", sessionId, e.getMessage());
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> log.debug("SSE completed for session {}", sessionId));
        emitter.onTimeout(() -> log.debug("SSE timed out for session {}", sessionId));
        emitter.onError(e -> log.warn("SSE error for session {}: {}", sessionId, e.getMessage()));

        return emitter;
    }
}
