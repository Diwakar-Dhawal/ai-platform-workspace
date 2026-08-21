package com.aiservice.platform.aiplatform.controller;

import com.aiservice.platform.aiplatform.model.CompletionRequest;
import com.aiservice.platform.aiplatform.model.CompletionResponse;
import com.aiservice.platform.aiplatform.service.CompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/completions")
@RequiredArgsConstructor
public class CompletionController {

    private final CompletionService completionService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        String appId = (String) request.get("appId");
        String message = (String) request.get("message");
        String sessionStr = (String) request.get("contentSessionId");
        UUID contentSessionId = sessionStr != null ? UUID.fromString(sessionStr) : null;

        CompletionRequest completionRequest = CompletionRequest.builder()
                .appId(appId)
                .message(message)
                .contentSessionId(contentSessionId)
                .build();

        log.info("Completion request: appId={}, sessionId={}, message='{}'",
                appId, contentSessionId, message != null && message.length() > 80
                        ? message.substring(0, 80) + "..." : message);

        CompletionResponse response = completionService.complete(completionRequest);

        log.info("Completion response: model={}, answer={} chars",
                response.getModel(), response.getAnswer() != null ? response.getAnswer().length() : 0);

        return Map.of(
                "answer", response.getAnswer(),
                "model", response.getModel(),
                "appId", response.getAppId()
        );
    }
}
